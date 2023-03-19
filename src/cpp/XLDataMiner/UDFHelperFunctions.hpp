#ifndef _UDFHELPERFUNCTIONS_H_INCLUDED_
#define _UDFHELPERFUNCTIONS_H_INCLUDED_

#include <string>
#include <vector>
#include <regex>
#include <future>
#include <Windows.h>
#include "StringUtils.hpp"
#include "TreeNode.hpp"
#include "xlcall.h"
#include "dtoa.h"
#include "globals.h"

inline bool isValidVariableName(const std::string& str)
{
	// Starts with a letter
	if (!isalpha(str[0]))
	{
		return false;
	}
	// Contain only chars in the set below
	else if (!std::regex_match(str, std::regex("^[a-zA-Z0-9_]+$")))
	{
		return false;
	}
	return true;
}

std::pair<size_t, size_t> getCallerRowsAndColumns()
{
	XLOPER12 xRes;
	Excel12(xlfCaller, &xRes, 0);
	size_t rows = xRes.val.sref.ref.rwLast - xRes.val.sref.ref.rwFirst + 1;
	size_t cols = xRes.val.sref.ref.colLast - xRes.val.sref.ref.colFirst + 1;
	Excel12(xlFree, 0, 1, (LPXLOPER12)&xRes);
	return std::pair<size_t, size_t> { rows, cols };
}

inline static std::string updateVariablesToSend(const LPXLOPER12 variables)
{
	if (variables->xltype != xltypeMulti || variables->val.array.columns < 2)
	{
		return "{}";
	}

	size_t cols = 2;
	size_t rows = variables->val.array.rows;
	size_t rwcols = cols * rows;
	TreeNode params{ "root", nullptr };
	TreeNode* ptr = &params;
	for (size_t i = 0; i < rwcols; i = i + 2)
	{
		if (variables->val.array.lparray[i].xltype != xltypeStr)
		{
			continue;
		}
		std::string str{ rawPwstringTostring(variables->val.array.lparray[i].val.str) };
		std::vector<std::string> nodes = split(str, '.');
		bool allNamesValid = true;
		for (const auto& node : nodes)
		{
			allNamesValid &= isValidVariableName(node);
		}
		if (allNamesValid)
		{
			size_t size = nodes.size() - 1;
			for (size_t j = 0; j < size; ++j)
			{
				ptr = ptr->insert(TreeNode{ nodes[j], ptr });
			}
			std::string nodeData = XLOPER12ToString(&variables->val.array.lparray[i + 1]);
			if (nodeData[1] == '[' && nodeData[nodeData.length() - 2] == ']') {
				nodeData = nodeData.substr(1, nodeData.length() - 2);
			}
			ptr->insert(TreeNode{ nodes[size], nodeData, ptr });
			// reset to root
			ptr = &params;
		}
	}
	return params.toJSON();
}

inline std::string buildDataIndexTable(const std::vector<std::string>& data, int segmentLength, int lastSegmentLength, int offset = 1)
{
	size_t i = offset;
	std::string tableEntries;
	size_t begin = 0;

	size_t end = data[i].length() - 1;

	while (true)
	{
		tableEntries += '{';
		tableEntries += "\"entry";
		tableEntries += std::to_string(i - offset);
		tableEntries += "\":{\"begin\":";
		tableEntries += std::to_string(begin);
		tableEntries += ",\"end\":";
		tableEntries += std::to_string(end);
		tableEntries += ",\"numentries\":";
		tableEntries += std::to_string(i < data.size() - 1 ? segmentLength : lastSegmentLength);
		tableEntries += "}},";

		++i;
		if (i == data.size())
		{
			break;
		}
		begin = end + 1;
		end = begin + data[i].length() - 1;
	}
	if (!tableEntries.empty())
	{
		tableEntries.pop_back();
	}
	return tableEntries;
}


inline std::vector<std::string> buildMessage(const LPXLOPER12 cells, const LPXLOPER12 variables, size_t outputRows, size_t outputCols)
{
	if (globals::pool_ptr == nullptr)
	{
		globals::pool_ptr = std::make_unique<ctpl::thread_pool>(std::thread::hardware_concurrency());
	}

	LPXLOPER12 cellArray;

	size_t rows;
	size_t cols;
	// if cells is just a single number
	if (cells->xltype != xltypeMulti)
	{
		XLOPER12 tmp[1] = { {cells->val, cells->xltype} };
		cellArray = tmp;
		rows = 1;
		cols = 1;
	}
	else
	{
		cellArray = cells->val.array.lparray;
		rows = cells->val.array.rows;
		cols = cells->val.array.columns;
	}

	std::string headers = ",\"headers\":[";
	for (size_t i = 0; i < cols; ++i)
	{
		headers +=  XLOPER12ToString(&cellArray[i]) + ",";

	}
	headers.back() = ']';
	// Move the pointer to the next row to skip the headers
	cellArray += cols;
	--rows;

	std::vector<std::string> result{ 1 };

	//uid = gen_random(32);
	GUID guid;
	CoCreateGuid(&guid);

	wchar_t szGUID[64] = { 0 };
	StringFromGUID2(guid, szGUID, 64);
	std::wstring wide{ szGUID };

	std::string str;
	std::transform(wide.begin(), wide.end(), std::back_inserter(str), [](wchar_t c) {
		return (char)c;
	});

	size_t numberOfCells(rows * cols);
	result.front() += "{\"inputdata\":{\"rows\":";
	result.front() += std::to_string(rows);
	result.front() += ",\"columns\":";
	result.front() += std::to_string(cols);
	result.front() += "},\"outputdata\":{\"rows\":";
	result.front() += std::to_string(outputRows);
	result.front() += ",\"columns\":";
	result.front() += std::to_string(outputCols);
	result.front() += "},\"variables\":";
	result.front() += updateVariablesToSend(variables);
	result.front() += headers;
	result.front() += ",\"GUID\":\"" + str + "\"";
	result.front() += ",\"dataindextable\":[";

	size_t noTasks{ std::min<size_t>(std::thread::hardware_concurrency(), rows) };

	std::vector<std::future<std::string>> futures;
	size_t segmentLength{ rows / noTasks * cols };
	size_t lastSegmentLength = segmentLength + (rows % noTasks) * cols;
	for (size_t i = 0; i < noTasks; ++i)
	{
		size_t begin = i * segmentLength;
		size_t end = i < noTasks - 1 ? segmentLength : lastSegmentLength;
		futures.emplace_back(globals::pool_ptr->push([cellArray, begin, end](int)
		{
			// The dtoa algorithm ensures that the converted number will never
			// exceed 24 chars
			char buf[24];
			std::string result;
			// Assuming each cell will need 10 bytes when converted to a string
			result.reserve(end * 10);
			for (size_t j = begin; j < begin + end; ++j)
			{
				XLOPER12ToString(&cellArray[j], result, buf);
				result += ',';
			}
			return result;
		}));
	}

	for (auto& future : futures)
	{
		result.emplace_back(future.get());
	}
	result.front() += buildDataIndexTable(result, segmentLength, lastSegmentLength);

	result.front() += "],\"data\":[";
	result.back().back() = ']';
	result.emplace_back("}");
	return result;
}

int getXLErrorCode(const char* str)
{
	int errorCode;
	if (!strcmp(str, "\"#DIV/0!\""))
	{
		errorCode = xlerrDiv0;
	}
	else if (!strcmp(str, "\"#N/A!\""))
	{
		errorCode = xlerrNA;
	}
	else if (!strcmp(str, "\"#NAME!\""))
	{
		errorCode = xlerrName;
	}
	else if (!strcmp(str, "\"#NULL!\""))
	{
		errorCode = xlerrNull;
	}
	else if (!strcmp(str, "\"#REF!\""))
	{
		errorCode = xlerrRef;
	}
	else if (!strcmp(str, "\"#NUM!\""))
	{
		errorCode = xlerrNum;
	}
	else if (!strcmp(str, "\"#VALUE!\""))
	{
		errorCode = xlerrValue;
	}
	else
	{
		errorCode = -1;
	}
	return errorCode;
}

#endif // _UDFHELPERFUNCTIONSH_INCLUDED_