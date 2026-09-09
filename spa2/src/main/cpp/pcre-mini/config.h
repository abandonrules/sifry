/* config.h for the Android (bionic) build of PCRE 8.45 — hand-tuned from
 * pcre-8.45 config.h.generic so the library is built interpreter-only with
 * UTF-8 + Unicode property support, matching the previous vendored 8.31.
 * This file is included by every source via -DHAVE_CONFIG_H (see CMakeLists). */

/* #undef BSR_ANYCRLF */
/* #undef EBCDIC */
/* #undef HAVE_BCOPY */
/* #undef HAVE_BITS_TYPE_TRAITS_H */
/* #undef HAVE_BZLIB_H */
/* #undef HAVE_DIRENT_H */
/* #undef HAVE_DLFCN_H */
/* #undef HAVE_EDITLINE_READLINE_H */
/* #undef HAVE_EDIT_READLINE_READLINE_H */
#define HAVE_INTTYPES_H 1
#define HAVE_LIMITS_H 1
#define HAVE_LONG_LONG 1
#define HAVE_MEMMOVE 1
#define HAVE_MEMORY_H 1
/* #undef HAVE_READLINE_HISTORY_H */
/* #undef HAVE_READLINE_READLINE_H */
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRERROR 1
#define HAVE_STRING 1
#define HAVE_STRINGS_H 1
#define HAVE_STRING_H 1
/* #undef HAVE_STRTOIMAX */
/* #undef HAVE_STRTOLL */
/* #undef HAVE_STRTOQ */
/* #undef HAVE_SYS_STAT_H */
/* #undef HAVE_SYS_TYPES_H */
/* #undef HAVE_TYPE_TRAITS_H */
#define HAVE_UNISTD_H 1
#define HAVE_UNSIGNED_LONG_LONG 1
/* #undef HAVE_WINDOWS_H */
/* #undef HAVE_ZLIB_H */
/* #undef HAVE__STRTOI64 */

#ifndef LT_OBJDIR
#define LT_OBJDIR ".libs/"
#endif

#ifndef LINK_SIZE
#define LINK_SIZE 2
#endif

#ifndef MATCH_LIMIT
#define MATCH_LIMIT 100000
#endif

#ifndef MATCH_LIMIT_RECURSION
#define MATCH_LIMIT_RECURSION MATCH_LIMIT
#endif

#ifndef MAX_NAME_COUNT
#define MAX_NAME_COUNT 10000
#endif

#ifndef MAX_NAME_SIZE
#define MAX_NAME_SIZE 32
#endif

#ifndef NEWLINE
#define NEWLINE 10
#endif

/* The value of PARENS_NEST_LIMIT specifies the maximum depth of nested
   parentheses (of any kind) in a pattern. This limits the amount of system
   stack used when compiling a pattern, and also limits the amount of heap
   memory. It was introduced in PCRE 8.41 to fix CVE-2017-7186. The default
   is 250. */
#ifndef PARENS_NEST_LIMIT
#define PARENS_NEST_LIMIT 250
#endif

/* #undef NO_RECURSE */

#define PACKAGE "pcre"
#define PACKAGE_BUGREPORT ""
#define PACKAGE_NAME "PCRE"
#define PACKAGE_STRING "PCRE 8.45"
#define PACKAGE_TARNAME "pcre"
#define PACKAGE_URL ""
#define PACKAGE_VERSION "8.45"

#ifndef PCREGREP_BUFSIZE
#define PCREGREP_BUFSIZE 20480
#endif

/* #undef PCRE_EXP_DEFN */
/* #undef PCRE_STATIC */

#ifndef POSIX_MALLOC_THRESHOLD
#define POSIX_MALLOC_THRESHOLD 10
#endif

#define STDC_HEADERS 1

/* #undef SUPPORT_JIT */
/* #undef SUPPORT_LIBBZ2 */
/* #undef SUPPORT_LIBEDIT */
/* #undef SUPPORT_LIBREADLINE */
/* #undef SUPPORT_LIBZ */
/* #undef SUPPORT_PCRE16 */
/* #undef SUPPORT_PCRE32 */
#define SUPPORT_PCRE8 /**/
/* #undef SUPPORT_PCREGREP_JIT */
#define SUPPORT_UCP /**/
#define SUPPORT_UTF /**/

#define VERSION "8.45"

/* #undef const */
/* #undef int64_t */
/* #undef size_t */