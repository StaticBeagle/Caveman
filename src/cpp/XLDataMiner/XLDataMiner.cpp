#include "WSClient.hpp"
#include <windows.h>
#include "xlcall.h"
#include "framewrk.h"
#include <assert.h>
#include <string>
#include <exception>
#include "StringUtils.hpp"
#include "UDFHelperFunctions.hpp"

#pragma warning( disable : 4996 )
#pragma warning( disable : 4267 )
/*
** rgFuncs
**
** This is a table of all the functions exported by this module.
** These functions are all registered (in xlAutoOpen) when you
** open the XLL. Before every string, leave a space for the
** byte count. The format of this table is the same as
** the last seven arguments to the REGISTER function.
** rgFuncsRows define the number of rows in the table. The
** dimension [3] represents the number of columns in the table.
*/
#define rgFuncsRows 29

static LPWSTR rgFuncs[rgFuncsRows][7] = {
	{ L"Caveman",					L"QQQH", L"Caveman" },
};


static unsigned short serverPortNumber = 8000;
static std::string uid;

//__declspec(dllexport) char*  WINAPI Cavenet(unsigned short portNumber = 80, char* queryString = "")
//{
//	if (portNumber > 0)
//	{
//		std::string status("Connected\n");
//		size_t length = status.length() + 1;
//		char* connection = new char[length];
//		strcpy_s(connection, length, status.c_str());
//		return connection;
//	}
//	std::string status("Not Connected\n");
//	size_t length = status.length() + 1;
//	char* connection = new char[length];
//	strcpy_s(connection, length, status.c_str());
//	return connection;
//}

LPXLOPER12 fillUpCellsWithErrorMessage(size_t selectedRows, size_t selectedColumns, wchar_t* errorMessage)
{
	size_t rwcol = selectedRows * selectedColumns;

	LPXLOPER12 xlMulti = new (std::nothrow) XLOPER12;
	if (xlMulti == 0)
	{
		return NULL;
	}

	LPXLOPER12 pxArray = new (std::nothrow) XLOPER12[rwcol];
	if (pxArray == 0)
	{
		delete xlMulti;
		return NULL;
	}

	// Initialize to safe default
	xlMulti->xltype = xltypeMulti | xlbitDLLFree;
	xlMulti->val.array.rows = selectedRows;
	xlMulti->val.array.columns = selectedColumns;
	xlMulti->val.array.lparray = pxArray;
	for (size_t i = 0; i < rwcol; ++i)
	{
		pxArray[i].xltype = xltypeStr;
		pxArray[i].val.str = deep_copy_wcs(errorMessage);
	}
	return xlMulti;
}

template<typename T1, typename T2, typename T3>
class DataIndexEntry
{
public:
	T1 BeginIndex;
	T2 EndIndex;
	T3 EntryCount;

	DataIndexEntry(T1 begin, T2 end, T3 count)
		: BeginIndex{ begin },
		EndIndex{ end },
		EntryCount{ count }
	{}
};

void parseServerData(char* str, size_t start, size_t end, LPXLOPER12 pxArray)
{
	for (size_t i = start; i < end; ++i)
	{
		char* begin = str;
		// replace ']' in str with ',' and then we can remove the && mwahaha
		while (*str != ',')
		{
			++str;
		}
		// Making a null terminated string
		*str = '\0';
		if (*begin == '\"')
		{
			// Cheking if string is an xl error type
			int errorCode = getXLErrorCode(begin);
			if (errorCode >= 0)
			{
				pxArray[i].xltype = xltypeErr;
				pxArray[i].val.err = errorCode;
			}
			else
			{
				// Making size prefixed (P-String) string
				*begin = str - begin - 2;

				pxArray[i].xltype = xltypeStr;
				pxArray[i].val.str = deep_copy_wcs(begin);
			}
		}
		else if (!strcmp(begin, "true"))
		{
			pxArray[i].xltype = xltypeBool;
			pxArray[i].val.xbool = true;
		}
		else if (!strcmp(begin, "false"))
		{
			pxArray[i].xltype = xltypeBool;
			pxArray[i].val.xbool = false;
		}
		else
		{
			pxArray[i].xltype = xltypeNum;
			pxArray[i].val.num = strtod(begin, nullptr);
		}
		// need to add error types
		++str;
		// there could be more cases now I'm stopping at double
		// also we can set the cells here and free the memory locally
		// instead of leaving that task to excel
	}
}


LPXLOPER12 processServerMessage(size_t selectedRows, size_t selectedColumns, char* serverMessage, size_t serverMessageLength)
{
	if (serverMessageLength == 0)
	{
		return fillUpCellsWithErrorMessage(selectedRows, selectedColumns, L"\031Server response was empty");
	}

	char* begin = strstr(serverMessage, "\"rows\":") + strlen("\"rows\":");
	serverMessage[serverMessageLength - 2] = ',';
	char* end = strchr(begin, ',');
	size_t rows = std::stol(std::string(begin, end - begin));

	begin = strstr(end, "\"columns\":") + strlen("\"columns\":");
	end = strchr(begin, ',');

	size_t cols = std::stol(std::string(begin, end - begin));

	begin = strstr(end, "\"headers\":[") + strlen("\"headers\":[");
	end = strchr(begin, ']');
	// Let's check if headers are not empty
	std::string headers{ begin, end };
	std::vector<std::string> tokens;
	if (!headers.empty())
	{
		tokens = split(headers, ',');
		++rows;
	}

	begin = strstr(end, "\"dataindextable\":[") + strlen("\"dataindextable\":[");
	end = strchr(begin, ']');

	char* str = strstr(end, "\"data\":[") + strlen("\"data\":[");

	std::vector<DataIndexEntry<int, int, int>> entries;
	std::string indexTable{ begin, end };

	int runningIndex = 0;
	size_t totalNumberOfEntries = 0;
	while (indexTable.find("entry", runningIndex) != std::string::npos)
	{
		int indexBegin = indexTable.find("\"begin\":", runningIndex) + strlen("\"begin\":");
		int indexEnd = indexTable.find("\"end\":", indexBegin) + strlen("\"end\":");
		runningIndex = indexTable.find("\"numentries\":", indexEnd) + strlen("\"numentries\":");

		int item1 = stoi(indexTable.substr(indexBegin, indexTable.find(',', indexBegin)));
		int item2 = stoi(indexTable.substr(indexEnd, indexTable.find(',', indexEnd)));
		int item3 = stoi(indexTable.substr(runningIndex, indexTable.find('}', runningIndex)));
		entries.emplace_back(item1, item2, item3);
		totalNumberOfEntries += item3;
	}

	size_t rwcol{ rows * cols };
	if (totalNumberOfEntries != rwcol)
	{
		// fill up with error and return
		// Error: inconsistent number of entries. The total number of entries must be equal to the number of rows times the number of columns
		// Total number of Entries: blah blah
		// Rows: blah
		// Cols: blah
	}
	

	LPXLOPER12 xlMulti = new (std::nothrow) XLOPER12;
	if (xlMulti == 0)
	{
		return NULL;
	}

	LPXLOPER12 pxArray = new (std::nothrow) XLOPER12[rwcol];
	if (pxArray == 0)
	{
		delete xlMulti;
		return NULL;
	}

	// Initialize to safe default
	xlMulti->xltype = xltypeMulti | xlbitDLLFree;
	xlMulti->val.array.rows = rows;
	xlMulti->val.array.columns = cols;

	xlMulti->val.array.lparray = pxArray;
	for (size_t i = 0; i < tokens.size(); ++i)
	{
		tokens[i][0] = tokens[i].length() - 2;

		pxArray[i].xltype = xltypeStr;
		pxArray[i].val.str = deep_copy_wcs(tokens[i].c_str());
	}



	std::vector<std::future<void>> futures;
	for (size_t i = 0; i < entries.size(); ++i)
	{
		size_t beginIndex = i * entries[0].EntryCount + tokens.size();
		size_t endIndex = beginIndex + entries[i].EntryCount;
		size_t offset = entries[i].BeginIndex;
		futures.emplace_back(globals::pool_ptr->push([&str, offset, beginIndex, endIndex, pxArray](int)
		{
			char* currentPos = &str[offset];
			for (size_t i = beginIndex; i < endIndex; ++i)
			{

				char* begin = currentPos;
				// replace ']' in str with ',' and then we can remove the && mwahaha
				while (*currentPos != ',')
				{
					++currentPos;
				}
				// Making a null terminated string
				*currentPos = '\0';
				if (*begin == '\"')
				{
					// Cheking if string is an xl error type
					int errorCode = getXLErrorCode(begin);
					if (errorCode >= 0)
					{
						pxArray[i].xltype = xltypeErr;
						pxArray[i].val.err = errorCode;
					}
					else
					{
						// Making size prefixed (P-String) string
						*begin = currentPos - begin - 2;

						pxArray[i].xltype = xltypeStr;
						pxArray[i].val.str = deep_copy_wcs(begin);
					}
				}
				else if (!strcmp(begin, "true"))
				{
					pxArray[i].xltype = xltypeBool;
					pxArray[i].val.xbool = true;
				}
				else if (!strcmp(begin, "false"))
				{
					pxArray[i].xltype = xltypeBool;
					pxArray[i].val.xbool = false;
				}
				else
				{
					pxArray[i].xltype = xltypeNum;
					pxArray[i].val.num = strtod(begin, nullptr);
				}
				// need to add error types
				++currentPos;
				// there could be more cases now I'm stopping at double
				// also we can set the cells here and free the memory locally
				// instead of leaving that task to excel
			}
		}));
	}
	for (auto& future : futures)
	{
		future.get();
	}


	return xlMulti;
}

static std::unique_ptr<WSClient> client_ptr{ nullptr };
LPXLOPER12 CaveGrunt(const std::vector<std::string>& multiMessage, unsigned int port)
{
	if (client_ptr == nullptr)
	{
		//MessageBoxW(NULL, L"Meow Mix", L"Some good title", MB_OK | MB_SETFOREGROUND);
		client_ptr = std::make_unique<WSClient>(port);
	}
	if (!client_ptr->isConnected())
	{
		client_ptr->connect();
	}

	LPXLOPER12 result = 0;
	// What cells called this function i.e. the cells expecting the result
	// from this function
	XLOPER12 xRes;
	Excel12(xlfCaller, &xRes, 0);
	size_t rows = xRes.val.sref.ref.rwLast - xRes.val.sref.ref.rwFirst + 1;
	size_t cols = xRes.val.sref.ref.colLast - xRes.val.sref.ref.colFirst + 1;
	if (client_ptr->getConnectionStatus() == ConnectionStatus::ServerDown)
	{
		result = fillUpCellsWithErrorMessage(rows, cols, L"\030Cannot connect to server");
	}
	else
	{
		ConnectionStatus status;
		std::string serverMessage;

		size_t beginSentGUID = multiMessage[0].find("\"GUID\"");
		size_t endSentGUID = multiMessage[0].find(",", beginSentGUID);
		std::string GUIDSent = multiMessage[0].substr(beginSentGUID, endSentGUID - beginSentGUID);

		size_t beginReceivedGUID;
		size_t endReceivedGUID;
		std::string GUIDReceived;

		{
			std::unique_lock<std::mutex> lk(WSClient::TransmitReceiveMutex);
			client_ptr->sendMessage(multiMessage);
			WSClient::TransmitReceiveConditionVariable.wait(lk, [] { return client_ptr->serverResponseReceivedOrTimedout; });

			status = client_ptr->getConnectionStatus();
			serverMessage = client_ptr->getServerMessage();

			if (!serverMessage.empty())
			{
				beginReceivedGUID = serverMessage.find("\"GUID\"");
				endReceivedGUID = serverMessage.find(",", beginReceivedGUID);
				GUIDReceived = serverMessage.substr(beginReceivedGUID, endReceivedGUID - beginReceivedGUID);
			}
		}

		if (status == ConnectionStatus::TimeOut)
		{
			result = fillUpCellsWithErrorMessage(rows, cols, L"\007Timeout");
		}
		else if (status == ConnectionStatus::InvalidAddress)
		{
			result = fillUpCellsWithErrorMessage(rows, cols, L"\013Invalid address");
		}
		else if (status == ConnectionStatus::Closed)
		{
			result = fillUpCellsWithErrorMessage(rows, cols, L"\042Connection closed by remote server");
		}
		else
		{
			if (GUIDSent == GUIDReceived && !serverMessage.empty())
			{
				result = processServerMessage(rows, cols, &serverMessage[0], serverMessage.length());
			}
		}
	}
	Excel12(xlFree, 0, 1, (LPXLOPER12)&xRes);
	client_ptr->serverResponseReceivedOrTimedout = false;
	return result;
}

__declspec(dllexport) LPXLOPER12 WINAPI Caveman(LPXLOPER12 cells, LPXLOPER12 variables, unsigned short port)
{
	if (cells->xltype == xltypeMissing)
	{
		return NULL;
	}
	if (port == 0)
	{
		port = 8675;
	}
	if (client_ptr != nullptr && client_ptr->getPort() != port)
	{
		client_ptr.reset();
		client_ptr = nullptr;
	}
	auto outputDims = getCallerRowsAndColumns();
	return CaveGrunt(buildMessage(cells, variables, outputDims.first, outputDims.second), port);
}





///////////////////////////////////////////////////////////////////////////////////////////////////


/////////////////////////////////////////////////////////////////////////////////////////////////////
//#define MYMENU_EXIT         (WM_APP + 101)
//#define MYMENU_MESSAGEBOX   (WM_APP + 102) 
//
//HINSTANCE  inj_hModule;          //Injected Modules Handle
//HWND       prnt_hWnd;            //Parent Window Handle
//
//								 //WndProc for the new window
//LRESULT CALLBACK DLLWindowProc(HWND, UINT, WPARAM, LPARAM);
//
////Register our windows Class
//BOOL RegisterDLLWindowClass(wchar_t szClassName[])
//{
//	WNDCLASSEX wc;
//	wc.hInstance = inj_hModule;
//	wc.lpszClassName = (LPCWSTR)L"InjectedDLLWindowClass";
//	wc.lpszClassName = (LPCWSTR)szClassName;
//	wc.lpfnWndProc = DLLWindowProc;
//	wc.style = CS_DBLCLKS;
//	wc.cbSize = sizeof(WNDCLASSEX);
//	wc.hIcon = LoadIcon(NULL, IDI_APPLICATION);
//	wc.hIconSm = LoadIcon(NULL, IDI_APPLICATION);
//	wc.hCursor = LoadCursor(NULL, IDC_ARROW);
//	wc.lpszMenuName = NULL;
//	wc.cbClsExtra = 0;
//	wc.cbWndExtra = 0;
//	wc.hbrBackground = (HBRUSH)COLOR_BACKGROUND;
//	if (!RegisterClassEx(&wc))
//		return 0;
//}
////Creating our windows Menu
//HMENU CreateDLLWindowMenu()
//{
//	HMENU hMenu;
//	hMenu = CreateMenu();
//	HMENU hMenuPopup;
//	if (hMenu == NULL)
//		return FALSE;
//	hMenuPopup = CreatePopupMenu();
//	AppendMenu(hMenuPopup, MF_STRING, MYMENU_EXIT, TEXT("Exit"));
//	AppendMenu(hMenu, MF_POPUP, (UINT_PTR)hMenuPopup, TEXT("File"));
//
//	hMenuPopup = CreatePopupMenu();
//	AppendMenu(hMenuPopup, MF_STRING, MYMENU_MESSAGEBOX, TEXT("MessageBox"));
//	AppendMenu(hMenu, MF_POPUP, (UINT_PTR)hMenuPopup, TEXT("Test"));
//	return hMenu;
//}
//
////The new thread
//DWORD WINAPI ThreadProc(LPVOID lpParam)
//{
//	MSG messages;
//	wchar_t *pString = reinterpret_cast<wchar_t * > (lpParam);
//	HMENU hMenu = CreateDLLWindowMenu();
//	RegisterDLLWindowClass(L"InjectedDLLWindowClass");
//	prnt_hWnd = FindWindow(L"Window Injected Into ClassName", L"Window Injected Into Caption");
//	HWND hwnd = CreateWindowEx(0, L"InjectedDLLWindowClass", pString, WS_EX_PALETTEWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 400, 300, prnt_hWnd, hMenu, inj_hModule, NULL);
//	ShowWindow(hwnd, SW_SHOWNORMAL);
//	while (GetMessage(&messages, NULL, 0, 0))
//	{
//		TranslateMessage(&messages);
//		DispatchMessage(&messages);
//	}
//	return 1;
//}
////Our new windows proc
//LRESULT CALLBACK DLLWindowProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
//{
//	switch (message)
//	{
//	case WM_COMMAND:
//		switch (wParam)
//		{
//		case MYMENU_EXIT:
//			SendMessage(hwnd, WM_CLOSE, 0, 0);
//			break;
//		case MYMENU_MESSAGEBOX:
//			MessageBox(hwnd, L"Test", L"MessageBox", MB_OK);
//			break;
//		}
//		break;
//	case WM_DESTROY:
//		PostQuitMessage(0);
//		break;
//	default:
//		return DefWindowProc(hwnd, message, wParam, lParam);
//	}
//	return 0;
//}

/*
** DllMain
**
** This function is called by LibEntry which is called
** by Windows when the DLL is first loaded. LibEntry initializes the
** DLL's heap if a HEAPSIZE is specified in the DLL's .DEF file, and
** then calls DllMain. The following DllMain function satisfies that
** call. The DllMain function should perform additional initialization
** tasks required by the DLL. In this example, we byte-count all the strings
** in the preceding table. DllMain will be called will be called once per
** process. In Win32 DllMain replaces both the LibMain and WEP functions.
**
** Arguments:
**
**      HANDLE hInstance			Instance handle
**      ULONG ul_reason_for_call	Reason DllMain was called
**      LPVOID lpReserved			Reserved
**
** Returns:
**
**      int                 1 if initialization is successful.
*/
BOOL WINAPI DllMain(HANDLE hInstance, ULONG ul_reason_for_call, LPVOID lpReserved)
{

	/* Deprecated due to MSVC handling of statically allocated strings */

	/*
	** In the following for loops, the strings in rgFuncs[] are byte-counted
	** so that they won't need to be byte-counted later.
	*/

	/*if(ul_reason_for_call == DLL_PROCESS_ATTACH)
	{
	int i,j;

	for (i = 0; i < rgFuncsRows; i++)
	{
	for (j = 0; j < 7; j++)
	{
	rgFuncs[i][j][0] = (BYTE)lstrlen (rgFuncs[i][j]+1);
	}
	}
	}*/
	//if (ul_reason_for_call == DLL_PROCESS_ATTACH) {
	//	inj_hModule = reinterpret_cast<HINSTANCE>(hInstance);
	//	CreateThread(0, NULL, ThreadProc, (LPVOID)L"Window Title", NULL, NULL);
	//}
	return 1;
}


/*
** xlAutoOpen
**
** xlAutoOpen is how Microsoft Excel loads XLL files.
** When you open an XLL, Microsoft Excel calls the xlAutoOpen
** function, and nothing more.
**
** More specifically, xlAutoOpen is called by Microsoft Excel:
**
**  - when you open this XLL file from the File menu,
**  - when this XLL is in the XLSTART directory, and is
**		automatically opened when Microsoft Excel starts,
**  - when Microsoft Excel opens this XLL for any other reason, or
**  - when a macro calls REGISTER(), with only one argument, which is the
**		name of this XLL.
**
** xlAutoOpen is also called by the Add-in Manager when you add this XLL
** as an add-in. The Add-in Manager first calls xlAutoAdd, then calls
** REGISTER("EXAMPLE.XLL"), which in turn calls xlAutoOpen.
**
** xlAutoOpen should:
**
**  - register all the functions you want to make available while this
**		XLL is open,
**
**  - add any menus or menu items that this XLL supports,
**
**  - perform any other initialization you need, and
**
**  - return 1 if successful, or return 0 if your XLL cannot be opened.
*/
extern __declspec(dllexport) int WINAPI xlAutoOpen(void)
{
	static XLOPER12 xDLL;	/* name of this DLL */
	int i;					/* Loop index */

							/*
							** In the following block of code the name of the XLL is obtained by
							** calling xlGetName. This name is used as the first argument to the
							** REGISTER function to specify the name of the XLL. Next, the XLL loops
							** through the rgFuncs[] table, registering each function in the table using
							** xlfRegister. Functions must be registered before you can add a menu
							** item.
							*/

	Excel12f(xlGetName, &xDLL, 0);

	for (i = 0; i < rgFuncsRows; i++)
	{
		Excel12f(xlfRegister, 0, 4,
			(LPXLOPER12)&xDLL,
			(LPXLOPER12)TempStr12(rgFuncs[i][0]),
			(LPXLOPER12)TempStr12(rgFuncs[i][1]),
			(LPXLOPER12)TempStr12(rgFuncs[i][2]));
	}

	/* Free the XLL filename */
	Excel12f(xlFree, 0, 1, (LPXLOPER12)&xDLL);
	return 1;
}




/*
** xlAutoClose
**
** xlAutoClose is called by Microsoft Excel:
**
**  - when you quit Microsoft Excel, or
**  - when a macro sheet calls UNREGISTER(), giving a string argument
**		which is the name of this XLL.
**
** xlAutoClose is called by the Add-in Manager when you remove this XLL from
** the list of loaded add-ins. The Add-in Manager first calls xlAutoRemove,
** then calls UNREGISTER("EXAMPLE.XLL"), which in turn calls xlAutoClose.
**
**
** xlAutoClose should:
**
**  - Remove any menus or menu items that were added in xlAutoOpen,
**
**  - do any necessary global cleanup, and
**
**  - delete any names that were added (names of exported functions, and
**		so on). Remember that registering functions may cause names to be created.
**
** xlAutoClose does NOT have to unregister the functions that were registered
** in xlAutoOpen. This is done automatically by Microsoft Excel after
** xlAutoClose returns.
**
** xlAutoClose should return 1.
*/
__declspec(dllexport) int WINAPI xlAutoClose(void)
{
	int i;

	/*
	** This block first deletes all names added by xlAutoOpen or by
	** xlAutoRegister.
	*/

	for (i = 0; i < rgFuncsRows; i++)
		Excel12f(xlfSetName, 0, 1, TempStr12(rgFuncs[i][2]));

	return 1;
}

///***************************************************************************
// lpwstricmp()
//
// Purpose: Compares a pascal string and a null-terminated C-string to see
// if they are equal.  Method is case insensitive
//
// Parameters:
//
//      LPWSTR s     First string (null-terminated)
//      LPWSTR t     Second string (byte counted)
//
// Returns: 
//
//      int         0 if they are equal
//                  Nonzero otherwise
//
// Comments:
//
//      Unlike the usual string functions, lpwstricmp
//      doesn't care about collating sequence.
//
// History:  Date       Author        Reason
///***************************************************************************

int lpwstricmp(LPWSTR s, LPWSTR t)
{
	int i;

	if (wcslen(s) != *t)
		return 1;

	for (i = 1; i <= s[0]; i++)
	{
		if (towlower(s[i - 1]) != towlower(t[i]))
			return 1;
	}
	return 0;
}

/*
** xlAutoRegister
**
** This function is called by Microsoft Excel if a macro sheet tries to
** register a function without specifying the type_text argument. If that
** happens, Microsoft Excel calls xlAutoRegister, passing the name of the
** function that the user tried to register. xlAutoRegister should use the
** normal REGISTER function to register the function, but this time it must
** specify the type_text argument. If xlAutoRegister does not recognize the
** function name, it should return a #VALUE! error. Otherwise, it should
** return whatever REGISTER returned.
**
** Arguments:
**
**	    LPXLOPER12 pxName   xltypeStr containing the
**                          name of the function
**                          to be registered. This is not
**                          case sensitive, because
**                          Microsoft Excel uses Pascal calling
**                          convention.
**
** Returns:
**
**      LPXLOPER12          xltypeNum containing the result
**                          of registering the function,
**                          or xltypeErr containing #VALUE!
**                          if the function could not be
**                          registered.
*/
__declspec(dllexport) LPXLOPER12 WINAPI xlAutoRegister12(LPXLOPER12 pxName)
{
	static XLOPER12 xDLL, xRegId;
	int i;

	/*
	** This block initializes xRegId to a #VALUE! error first. This is done in
	** case a function is not found to register. Next, the code loops through the
	** functions in rgFuncs[] and uses lpstricmp to determine if the current
	** row in rgFuncs[] represents the function that needs to be registered.
	** When it finds the proper row, the function is registered and the
	** register ID is returned to Microsoft Excel. If no matching function is
	** found, an xRegId is returned containing a #VALUE! error.
	*/

	xRegId.xltype = xltypeErr;
	xRegId.val.err = xlerrValue;

	for (i = 0; i < rgFuncsRows; i++)
	{
		if (!lpwstricmp(rgFuncs[i][0], pxName->val.str))
		{
			Excel12f(xlGetName, &xDLL, 0);

			Excel12f(xlfRegister, 0, 4,
				(LPXLOPER12)&xDLL,
				(LPXLOPER12)TempStr12(rgFuncs[i][0]),
				(LPXLOPER12)TempStr12(rgFuncs[i][1]),
				(LPXLOPER12)TempStr12(rgFuncs[i][2]));

			/* Free the XLL filename */
			Excel12f(xlFree, 0, 1, (LPXLOPER12)&xDLL);

			return (LPXLOPER12)&xRegId;
		}
	}

	//Word of caution - returning static XLOPERs/XLOPER12s is not thread safe
	//for UDFs declared as thread safe, use alternate memory allocation mechanisms

	return (LPXLOPER12)&xRegId;
}

/*
** xlAutoAdd
**
** This function is called by the Add-in Manager only. When you add a
** DLL to the list of active add-ins, the Add-in Manager calls xlAutoAdd()
** and then opens the XLL, which in turn calls xlAutoOpen.
**
*/
__declspec(dllexport) int WINAPI xlAutoAdd(void)
{
	XCHAR szBuf[255];

	wsprintfW((LPWSTR)szBuf, L"Thank you for adding Me\n build date %hs, time %hs", __DATE__, __TIME__);

	/* Display a dialog box indicating that the XLL was successfully added */
	Excel12f(xlcAlert, 0, 2, TempStr12(szBuf), TempInt12(2));
	return 1;
}

/*
** xlAutoRemove
**
** This function is called by the Add-in Manager only. When you remove
** an XLL from the list of active add-ins, the Add-in Manager calls
** xlAutoRemove() and then UNREGISTER("EXAMPLE.XLL").
**
** You can use this function to perform any special tasks that need to be
** performed when you remove the XLL from the Add-in Manager's list
** of active add-ins. For example, you may want to delete an
** initialization file when the XLL is removed from the list.
*/
__declspec(dllexport) int WINAPI xlAutoRemove(void)
{
	/* Display a dialog box indicating that the XLL was successfully removed */
	Excel12f(xlcAlert, 0, 2, TempStr12(L"Thank you for removing Me!"), TempInt12(2));
	return 1;
}

/* xlAddInManagerInfo12
**
**
** This function is called by the Add-in Manager to find the long name
** of the add-in. If xAction = 1, this function should return a string
** containing the long name of this XLL, which the Add-in Manager will use
** to describe this XLL. If xAction = 2 or 3, this function should return
** #VALUE!.
**
** Arguments
**
**      LPXLOPER12 xAction    The information you want; either
**                          1 = the long name of the
**                              add in, or
**                          2 = reserved
**                          3 = reserved
**
** Return value
**
**      LPXLOPER12            The long name or #VALUE!.
**
*/
__declspec(dllexport) LPXLOPER12 WINAPI xlAddInManagerInfo12(LPXLOPER12 xAction)
{
	static XLOPER12 xInfo, xIntAction;

	/*
	** This code coerces the passed-in value to an integer. This is how the
	** code determines what is being requested. If it receives a 1, it returns a
	** string representing the long name. If it receives anything else, it
	** returns a #VALUE! error.
	*/

	Excel12f(xlCoerce, &xIntAction, 2, xAction, TempInt12(xltypeInt));

	if (xIntAction.val.w == 1)
	{
		xInfo.xltype = xltypeStr;
		xInfo.val.str = L"\026XLData Miner";
	}
	else
	{
		xInfo.xltype = xltypeErr;
		xInfo.val.err = xlerrValue;
	}

	//Word of caution - returning static XLOPERs/XLOPER12s is not thread safe
	//for UDFs declared as thread safe, use alternate memory allocation mechanisms

	return (LPXLOPER12)&xInfo;
}



/*
** xlAutoFree
**
** Demonstrates the xlAutoFree callback. Frees the memory allocated by fArray as noted
** in the comment above.
**
*/

__declspec(dllexport) void WINAPI xlAutoFree12(LPXLOPER12 pxFree)
{
	if (pxFree->xltype & xltypeMulti)
	{
		// Assume all string elements were allocated using new, and
		// need to be freed using delete. Then free the array itself.
		int size = pxFree->val.array.rows * pxFree->val.array.columns;
		LPXLOPER12 p = pxFree->val.array.lparray;

		for (; size-- > 0; p++) // check elements for strings
		{
			if (p->xltype == xltypeStr)
			{
				delete[] p->val.str;
			}
		}
		delete[] pxFree->val.array.lparray;
	}
	else if (pxFree->xltype & xltypeStr)
	{
		delete[] pxFree->val.str;
	}
	else if (pxFree->xltype & xltypeRef)
	{
		delete[] pxFree->val.mref.lpmref;
	}
	// Assume pxFree was itself dynamically allocated using new.
	delete pxFree;
}