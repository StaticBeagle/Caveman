package com.wildbitsfoundry.caveman;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class CaveGrunt2D extends AbstractCaveGrunt
        implements ICaveGruntBehavior<List<CaveRow>, List<CaveRow>> {

    @Override
    public abstract void doGruntWork(final List<CaveRow> inputRows, List<CaveRow> outputRows);

    @Override
    CaveOutStreamBuilder doAllGruntWork(Iterable<CaveCell> caveDataStream, int numInputRows, int numInputCols,
            int numOutputRows, int numOutputCols) throws ExecutionException, InterruptedException {

        // split the input data into an array/list of rows
        List<Future<List<CaveRow>>> taskPool = new ArrayList<>();
        Spliterator<CaveCell> dataStream = caveDataStream.spliterator();
        for (;;) {
            Spliterator<CaveCell> subStream = dataStream.trySplit();
            if (subStream == null) {
                break;
            }

            Iterator<CaveCell> it = Spliterators.iterator(subStream);
            Callable<List<CaveRow>> task = () -> {

                List<CaveRow> rows = new ArrayList<>(Math.toIntExact(subStream.estimateSize()));
                while (it.hasNext()) {
                    final CaveRow currentRow = new CaveRow(new CaveCell[numInputCols]);
                    for (int i = 0; i < numInputCols; ++i) {
                        currentRow.set(i, it.next());
                    }
                    rows.add(currentRow);
                }
                return rows;
            };
            taskPool.add(this._threadPool.submit(task));
        }

        // Wait for all tasks to complete
        List<CaveRow> rows = new ArrayList<>();
        for (Future<List<CaveRow>> future : taskPool) {
            rows.addAll(future.get());
        }

        List<CaveRow> outputRows = new ArrayList<>(numOutputRows);
        this.doGruntWork(rows, outputRows);

        for (int i = 0; i < outputRows.size(); ++i) {
            CaveRow row = outputRows.get(i);
            if (row == null) {
                outputRows.set(i, new CaveRow(numOutputCols));
            } else {
                row.resize(numOutputCols);
            }
        }

        while (outputRows.size() < numOutputRows) {
            outputRows.add(new CaveRow(numOutputCols));
        }

        int dataSectionCount = Runtime.getRuntime().availableProcessors();
        int segmentLength = numOutputRows / dataSectionCount;
        int lastSegmentLength = segmentLength + numOutputRows % dataSectionCount;

        CaveOutStreamBuilder outStream = new CaveOutStreamBuilder();
        for (int i = 0; i < dataSectionCount; ++i) {
            int offset = i * segmentLength;
            if (i == dataSectionCount - 1) {
                segmentLength = lastSegmentLength;
            }
            // Assuming each cell will need 10 characters when converted to a string
            CaveOutStreamBuffer streamBuffer = new CaveOutStreamBuffer(segmentLength * 10);
            for (int j = 0; j < segmentLength; ++j) {
                CaveRow row = outputRows.get(offset);
                streamBuffer.append(row);
                offset++;
            }
            outStream.append(streamBuffer);
        }
        outStream.append(numOutputRows, numOutputCols);
        return outStream;
    }
}
