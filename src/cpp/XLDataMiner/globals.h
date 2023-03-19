#ifndef _GLOBALS_H_INCLUDED_
#define _GLOBALS_H_INCLUDED_

#include "ctpl_stl.h"

namespace globals
{
	extern std::unique_ptr<ctpl::thread_pool> pool_ptr{ nullptr };
}

#endif // _GLOBALS_H_INCLUDED_