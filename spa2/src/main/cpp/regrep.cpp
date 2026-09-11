#include <cstddef>
#include <string>
#include <exception>
#include <stdexcept>
#include <system_error>
#include <vector>
#include <utility>
#include <thread>
#include <mutex>
#include <atomic>
#include <algorithm>

#include <jni.h>

#include "wrappers.h"

constexpr const char* RetClass = "cz/absolutno/sifry/regexp/RegExpNative$Report";

class Context {
    std::thread worker{};
    volatile std::atomic_bool stopFlag{false};
    bool running{false};

    std::vector<std::string> matches{};
    unsigned maxListResults;
    unsigned matchCount{0};  // the full count: the vector will only store first maxListResults
    bool full{false};        // true: store the whole line (key + display), false: display only
    std::string error{};
    float progress{0};

    std::mutex progressMutex{};

    struct Report {
        bool running;
        float progress;
        bool error;
        unsigned matchCount;
    };

public:
    Context(unsigned max) : maxListResults(max) { }

    ~Context() {
        stop();
    }

    void start(std::vector<AssetRef>&& assets, std::vector<std::string>&& patterns, bool full, unsigned maxListResults);
    void stop();
    void free();
    void fail(const std::string& err);

    Report report();

    std::string getError() {
        std::lock_guard<std::mutex> lock{progressMutex};
        return error;
    }

    std::string getMatch(std::size_t index);

private:
    static void threadMain(Context* ctx, std::vector<AssetRef>&& assets, std::vector<std::string>&& patterns);
};


void Context::start(std::vector<AssetRef>&& assets, std::vector<std::string>&& patterns, bool full, unsigned max) {
    free();
    this->full = full;
    maxListResults = max;
    try {
        {
            std::lock_guard<std::mutex> lock{progressMutex};
            running = true;
        }
        worker = std::thread{threadMain, this, std::move(assets), std::move(patterns)};
    }
    catch(const std::system_error&) {
        std::lock_guard<std::mutex> lock{progressMutex};
        error = "Unable to start thread";
        running = false;
    }
}

void Context::stop() {
    if(!worker.joinable())
        return;
    stopFlag = true;
    worker.join();
    stopFlag = false;
}

void Context::free() {
    stop();
    std::lock_guard<std::mutex> lock{progressMutex};
    matches.clear();
    matchCount = 0;
    progress = 0;
    running = false;
    error.clear();
}

void Context::fail(const std::string &err) {
    free();
    std::lock_guard<std::mutex> lock{progressMutex};
    error = err;
}

Context::Report Context::report() {
    std::lock_guard<std::mutex> lock{progressMutex};
    return {running, progress, !error.empty(), matchCount};
}

std::string Context::getMatch(std::size_t index) {
    std::lock_guard<std::mutex> lock{progressMutex};
    return index < matches.size() ? matches[index] : "";
}


std::string strFromJava(JNIEnv *env, jstring jfn) {
    const char *cfn = env->GetStringUTFChars(jfn, 0);
    std::string ret{cfn};
    env->ReleaseStringUTFChars(jfn, cfn);
    return ret;
}

Context *getContext(JNIEnv *env, jobject obj) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fld = env->GetFieldID(cls, "nativeContext", "J");
    void *addr = reinterpret_cast<void *>(env->GetLongField(obj, fld));
    return static_cast<Context *>(addr);
}

void storeContext(JNIEnv *env, jobject obj, Context *ctx) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fld = env->GetFieldID(cls, "nativeContext", "J");
    jlong addr = reinterpret_cast<jlong>(ctx);
    env->SetLongField(obj, fld, addr);
}


extern "C" {

JNIEXPORT void JNICALL
Java_cz_absolutno_sifry_regexp_RegExpNative_init(JNIEnv *env, jobject obj) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fld = env->GetStaticFieldID(cls, "MaxListResults", "I");
    unsigned maxListResults = (unsigned)env->GetStaticIntField(cls, fld);
    storeContext(env, obj, new Context(maxListResults));
}

JNIEXPORT void JNICALL Java_cz_absolutno_sifry_regexp_RegExpNative_free(JNIEnv *env, jobject obj) {
    getContext(env, obj)->free();
}

JNIEXPORT void JNICALL
Java_cz_absolutno_sifry_regexp_RegExpNative_nativeFinalize(JNIEnv *env, jobject obj) {
    Context* ctx = getContext(env, obj);
    if(ctx == nullptr)
        return;
    storeContext(env, obj, nullptr);
    ctx->stop();
    delete ctx;
}

JNIEXPORT void JNICALL Java_cz_absolutno_sifry_regexp_RegExpNative_startThread(JNIEnv *env, jobject obj, jobject jmgr, jobjectArray jfns, jobjectArray joa, jboolean jfull, jint jmax) {
    Context* ctx = getContext(env, obj);
    ctx->stop();
    try {
        jsize n = env->GetArrayLength(joa);
        std::vector<std::string> patterns{};
        for(jsize i = 0; i < n; i++) {
            jstring js = static_cast<jstring>(env->GetObjectArrayElement(joa, i));
            if(js == nullptr) {
                patterns.emplace_back();
                continue;
            }
            patterns.push_back(strFromJava(env, js));
            env->DeleteLocalRef(js);
        }
        std::vector<AssetRef> assets{};
        jsize nf = env->GetArrayLength(jfns);
        for(jsize i = 0; i < nf; i++) {
            jstring js = static_cast<jstring>(env->GetObjectArrayElement(jfns, i));
            if(js == nullptr)
                continue;
            std::string fn = strFromJava(env, js);
            env->DeleteLocalRef(js);
            assets.emplace_back(env, jmgr, fn);
        }
        ctx->start(std::move(assets), std::move(patterns), jfull == JNI_TRUE, (unsigned)jmax);
    } catch(std::runtime_error& e) {
        ctx->fail(e.what());
    }
}

JNIEXPORT void JNICALL Java_cz_absolutno_sifry_regexp_RegExpNative_stopThread(JNIEnv *env , jobject obj) {
    getContext(env, obj)->stop();
}

JNIEXPORT jboolean JNICALL Java_cz_absolutno_sifry_regexp_RegExpNative_isRunning(JNIEnv *env , jobject obj) {
    return static_cast<jboolean>(getContext(env, obj)->report().running ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT jobject JNICALL Java_cz_absolutno_sifry_regexp_RegExpNative_getProgress(JNIEnv *env , jobject obj) {
    Context* ctx = getContext(env, obj);
    auto report = ctx->report();
    jclass cls = env->FindClass(RetClass);
    jmethodID ctor = env->GetMethodID(cls, "<init>", "()V");
    jobject ret = env->NewObject(cls, ctor);
    jfieldID fld = env->GetFieldID(cls, "running", "Z");
    env->SetBooleanField(ret, fld, (jboolean)report.running);
    fld = env->GetFieldID(cls, "error", "Z");
    env->SetBooleanField(ret, fld, (jboolean)report.error);
    fld = env->GetFieldID(cls, "matches", "I");
    env->SetIntField(ret, fld, (jint)report.matchCount);
    fld = env->GetFieldID(cls, "progress", "F");
    env->SetFloatField(ret, fld, report.progress);
    return ret;
}

JNIEXPORT jstring JNICALL Java_cz_absolutno_sifry_regexp_RegExpNative_getResult(JNIEnv *env, jobject obj, jint index) {
    Context* ctx = getContext(env, obj);
    return env->NewStringUTF(ctx->getMatch(std::size_t(index)).c_str());
}

JNIEXPORT jstring JNICALL Java_cz_absolutno_sifry_regexp_RegExpNative_getError(JNIEnv *env, jobject obj) {
    Context* ctx = getContext(env, obj);
    return env->NewStringUTF(ctx->getError().c_str());
}

}


void Context::threadMain(Context* ctx, std::vector<AssetRef>&& assets, std::vector<std::string>&& patterns) {
    constexpr unsigned bufAlloc = 1024;

    try {
        std::vector<PCRE> REs;
        for (auto &pat : patterns) {
            if (pat.empty())
                continue;
            bool inv = false;
            if (pat.front() == '!') {
                inv = true;
                pat = pat.substr(1);
            }
            if (pat.empty())
                continue;
            REs.push_back({pat, !inv});
        }

        std::size_t totalSize = 0;
        for (auto& asset : assets)
            totalSize += size_t(AAsset_getLength(asset));

        {
            std::lock_guard<std::mutex> lock{ctx->progressMutex};
            ctx->matches.clear();
            ctx->matchCount = 0;
            ctx->running = true;
        }

        std::size_t cumPos = 0;
        for (auto& asset : assets) {
            char buffer[bufAlloc];
            unsigned bufPos = 0, bufSize = 0;
            std::size_t fileSize = size_t(AAsset_getLength(asset));

            while (!ctx->stopFlag) {
                /* Read one line */
                std::string line{};
                for (;;) {
                    unsigned x;
                    for (x = bufPos; x < bufSize; x++)
                        if (buffer[x] == '\n')
                            break;
                    if (x < bufSize) {
                        line += std::string{buffer + bufPos, x - bufPos};
                        bufPos = x + 1;
                        break;
                    }
                    /* else: buffer ended before finding \n */
                    line += std::string{buffer + bufPos, bufSize - bufPos};
                    int r = AAsset_read(asset, buffer, bufAlloc);
                    if (r > 0) /* more data */ {
                        bufPos = 0;
                        bufSize = unsigned(r);
                    } else if (r == 0) /* EOF*/ {
                        break;
                    } else /* error */ {
                        throw std::runtime_error("Can't read asset");
                    }
                }

                if (line.empty() && AAsset_getRemainingLength(asset) == 0) // EOF
                    break;
                if (line.empty()) // empty line
                    continue;

                if(std::all_of(REs.begin(), REs.end(), [&line](PCRE& regex) -> bool {
                    return regex.test(line);
                })) {
                    std::lock_guard<std::mutex> lock{ctx->progressMutex};
                    if(++ctx->matchCount <= ctx->maxListResults) {
                        if(ctx->full) {
                            ctx->matches.push_back(line);
                        } else {
                            auto pos = line.find(':');
                            if(pos != std::string::npos)
                                ctx->matches.push_back(line.substr(pos + 1));
                        }
                    }
                }

                std::size_t filePos = fileSize - size_t(AAsset_getRemainingLength(asset));
                float progress = totalSize == 0 ? 1.0f : float(cumPos + filePos) / float(totalSize);
                {
                    std::lock_guard<std::mutex> lock{ctx->progressMutex};
                    ctx->progress = progress;
                }
            }
            cumPos += fileSize;
            if (ctx->stopFlag)
                break;
        }
    } catch (const std::runtime_error& e) {
        std::lock_guard<std::mutex> lock{ctx->progressMutex};
        ctx->error = e.what();
    }
    {
        std::lock_guard<std::mutex> lock{ctx->progressMutex};
        ctx->running = false;
    }
}