//#ifndef _MESSAGEBUILDER_H_INCLUDED
//#define _MESSAGEBUILDER_H_INCLUDED
//
//#include <string>
//#include <vector>
//#include <regex>
//#include <future>
//#include "xlcall.h"
//#include "dtoa.h"
//
//class MessageBuilder
//{
//public:
//	std::vector<std::string >> buildMessage();
//};
//
//inline std::vector<std::string> createDataToSend(const FP12* array, const std::string& variables)
//{
//	size_t noTasks{ 4 };
//	std::vector<std::string> result{ 1 };
//	//uid = gen_random(32);
//
//	size_t numberOfCells(array->rows * array->columns);
//	result.front() += "{\"rows\":";
//	result.front() += std::to_string(array->rows);
//	result.front() += ",\"columns\":";
//	result.front() += std::to_string(array->columns);
//	//output += ",\"uid\":\"";
//	//output += uid;
//	result.front() += ",\"variables\":";
//	result.front() += variables;
//	result.front() += ",\"dataindextable\":";
//
//	// We need approx. 160 per data table entry
//	size_t indexBeginDataTable = result.front().length();
//
//	std::vector<std::future<std::string>> futures;
//	size_t segmentLength{ numberOfCells / noTasks };
//
//	for (size_t i = 0; i < noTasks; ++i)
//	{
//		futures.emplace_back(std::async(std::launch::async, [array](size_t begin, size_t end)
//		{
//			// The dtoa algorithm ensures that the converted number will never
//			// exceed 24 chars
//			char buf[24];
//			std::string result;
//			for (size_t j = begin; j < begin + end; ++j)
//			{
//				dtoa_milo(array->array[j], buf);
//				result += buf;
//				result += ',';
//			}
//			return result;
//		}, i * segmentLength, i < noTasks - 1 ? segmentLength : segmentLength + numberOfCells % noTasks));
//	}
//
//	for (auto& future : futures)
//	{
//		result.emplace_back(future.get());
//	}
//
//	std::string tableEntries{ "[" };
//	size_t begin = 0;
//	size_t end = result[1].length() - 1;
//	size_t i = 1;
//	while (true)
//	{
//		tableEntries += '{';
//		tableEntries += "\"entry";
//		tableEntries += std::to_string(i - 1);
//		tableEntries += "\":{\"begin\":";
//		tableEntries += std::to_string(begin);
//		tableEntries += ",\"end\":";
//		tableEntries += std::to_string(end);
//		tableEntries += ",\"numentries\":";
//		tableEntries += std::to_string(i < noTasks - 1 ? segmentLength : segmentLength + numberOfCells % noTasks);
//		tableEntries += "}},";
//
//		++i;
//		if (i == result.size())
//		{
//			break;
//		}
//		begin = end + 1;
//		end = begin + result[i].length() - 1;
//	}
//	tableEntries.back() = ']';
//	result.front() += tableEntries;
//
//	result.front() += ",\"data\":[";
//	result.back().back() = ']';
//	result.emplace_back("}");
//	return result;
//
//}
//
//#endif _MESSAGEBUILDER_H_INCLUDED
