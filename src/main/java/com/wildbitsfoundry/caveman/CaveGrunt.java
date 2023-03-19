package com.wildbitsfoundry.caveman;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class CaveGrunt extends AbstractCaveGrunt implements ICaveGruntBehavior<CaveRow, CaveRow> {

    @Override
    public abstract void doGruntWork(final CaveRow inputRow, CaveRow outputRow);

    @Override
    CaveOutStreamBuilder doAllGruntWork(Iterable<CaveCell> caveDataStream, int numInputRows, int numInputCols,
            int numOutputRows, int numOutputCols) throws ExecutionException, InterruptedException {
        List<Future<CaveOutStreamBuffer>> taskPool = new ArrayList<>();
        Spliterator<CaveCell> dataStream = caveDataStream.spliterator();
        for (;;) {
            Spliterator<CaveCell> subStream = dataStream.trySplit();
            if (subStream == null) {
                break;
            }

            Iterator<CaveCell> it = Spliterators.iterator(subStream);
            Callable<CaveOutStreamBuffer> task = () -> {
                // Assuming each cell will need 10 characters when converted to a string
                CaveOutStreamBuffer outStream = new CaveOutStreamBuffer(
                        Math.toIntExact(subStream.estimateSize()) * 10);

                final CaveRow currentRow = new CaveRow(new CaveCell[numInputCols]);
                while (it.hasNext()) {
                    for (int i = 0; i < numInputCols; ++i) {
                        currentRow.set(i, it.next());
                    }
                    CaveRow outputRow = new CaveRow(numOutputCols);
                    doGruntWork(currentRow, outputRow);
                    outStream.append(outputRow);
                }
                return outStream;
            };
            taskPool.add(this._threadPool.submit(task));
        }

        // Wait for all tasks to complete
        CaveOutStreamBuilder outStream = new CaveOutStreamBuilder();
        for (Future<CaveOutStreamBuffer> future : taskPool) {
            outStream.append(future.get());
        }
        outStream.append(numInputRows, numOutputCols);
        return outStream;
    }
}
