#ifndef _STRINGUTILS_H_INCLUDED_
#define _STRINGUTILS_H_INCLUDED_

#include <string>
#include <vector>
#include "xlcall.h"
#include "dtoa.h"

#define MAX_V12_STRBUFFLEN    32678

wchar_t* deep_copy_wcs(const wchar_t *p_source)
{
	if (!p_source)
		return NULL;

	int source_len = p_source[0];
	bool truncated = false;

	if (source_len >= MAX_V12_STRBUFFLEN)
	{
		source_len = MAX_V12_STRBUFFLEN - 1; // Truncate the copy
		truncated = true;
	}
	wchar_t *p_copy = new wchar_t[source_len + 1];
	wcsncpy(p_copy, p_source, source_len + 1);
	if (truncated)
		p_copy[0] = source_len;
	return p_copy;
}

wchar_t* deep_copy_wcs(const char* p_source)
{
	if (!p_source)
		return NULL;

	int source_len = p_source[0];
	bool truncated = false;

	if (source_len >= MAX_V12_STRBUFFLEN)
	{
		source_len = MAX_V12_STRBUFFLEN - 1; // Truncate the copy
		truncated = true;
	}
	wchar_t *p_copy = new wchar_t[source_len + 1];
	mbstowcs(p_copy, p_source, source_len + 1);
	if (truncated)
		p_copy[0] = source_len;
	return p_copy;
}

std::string rawPwstringTostring(const wchar_t* str)
{
	// wchar p-string to std::string
	size_t size = str[0];
	std::unique_ptr<char> copy(new char[size + 1]);
	wcstombs(copy.get(), ++str, size);
	copy.get()[size] = '\0';
	return std::string{ copy.get() };
}

std::vector<std::string> split(const std::string& str, char delimiter)
{
	
	std::vector<std::string> tokens;
	std::size_t start = 0, end = 0;
	while ((end = str.find(delimiter, start)) != std::string::npos)
	{
		tokens.emplace_back(str.substr(start, end - start));
		start = end + 1;
	}
	tokens.emplace_back(str.substr(start));
	return tokens;
}


std::string gen_random(const int len) {
	static const char alphanum[] =
		"0123456789"
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ"
		"abcdefghijklmnopqrstuvwxyz";

	std::string str;
	str.resize(len);
	for (int i = 0; i < len; ++i)
	{
		str[i] = alphanum[rand() % (sizeof(alphanum) - 1)];
	}
	return str;
}

inline void XLOPER12ToString(const LPXLOPER12 xloper, std::string& str, char* buffer)
{
	switch (xloper->xltype)
	{
		case(xltypeInt):
		{
			dtoa_milo(xloper->val.w, buffer);
			str += buffer;
			break;
		}
		case(xltypeNum):
		{
			dtoa_milo(xloper->val.num, buffer);
			str += buffer;
			break;
		}
		case(xltypeStr):
		{
			str += "\"";
			str += rawPwstringTostring(xloper->val.str);
			str += "\"";
			break;
		}
		case(xltypeMissing):
		{
			str += '0';
			break;
		}
		case(xltypeNil):
		{
			str += '0';
			break;
		}
		case(xltypeBool):
		{
			str += xloper->val.xbool == 0 ? "FALSE" : "TRUE";
			break;
		}
		case(xltypeErr):
		{
			// I know this is a bit on the wild side but here is my switch
			// inside a switch
			switch (xloper->val.err)
			{
				case(xlerrNull):
				{
					str += "\"#NULL!\"";
					break;
				}
				case(xlerrDiv0):
				{
					str += "\"#DIV/0!\"";
					break;
				}
				case(xlerrValue):
				{
					str += "\"#VALUE!\"";
					break;
				}
				case(xlerrRef):
				{
					str += "\"#REF!\"";
					break;
				}
				case(xlerrName):
				{
					str += "\"#NAME!\"";
					break;
				}
				case(xlerrNum):
				{
					str += "\"#NUM!\"";
					break;
				}
				case(xlerrNA):
				{
					str += "\"xlerrNA\"";
					break;
				}
				default:
				{
					str += "\"xlerrUnknown\"" ;
					break;
				}
			}
			break;
		}
		default:
		{
			str += '0' ;
			break;
		}
	}
}

inline std::string XLOPER12ToString(const LPXLOPER12 xloper)
{
	std::string str;
	char buffer[26];
	XLOPER12ToString(xloper, str, buffer);
	return str;
}

#endif // _STRINGUTILS_H_INCLUDED_