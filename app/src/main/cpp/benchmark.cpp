#include <jni.h>
#include <chrono>
#include <cstring>
#include <stdexcept>
#include <android/log.h>
#include <arm_neon.h>

#define LOG_TAG "RAMBench"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

double calculateThroughput(size_t size, double elapsed) {
    return (size / (1024.0 * 1024.0)) / elapsed;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_compucode_rambench_domain_BenchmarkService_benchmarkRead(JNIEnv* env, jobject obj, jint size) {
    char* buffer = nullptr;
    try {
        buffer = new char[size];
        memset(buffer, 1, size);

        volatile int64_t sum = 0;

        // Warm-up phase
        int64x2_t sum_vec_warmup = {0, 0};
        int size_vec = size / 16; // 16-byte chunks
        int64x2_t* buffer_vec = reinterpret_cast<int64x2_t*>(buffer);
        for (int i = 0; i < size_vec; i++) {
            sum_vec_warmup = vaddq_s64(sum_vec_warmup, buffer_vec[i]);
        }
        sum = vgetq_lane_s64(sum_vec_warmup, 0) + vgetq_lane_s64(sum_vec_warmup, 1);

        // Measurement phase
        auto start = std::chrono::high_resolution_clock::now();
        int64x2_t sum_vec = {0, 0};
        for (int i = 0; i < size_vec; i++) {
            sum_vec = vaddq_s64(sum_vec, buffer_vec[i]);
        }
        sum = vgetq_lane_s64(sum_vec, 0) + vgetq_lane_s64(sum_vec, 1);
        auto end = std::chrono::high_resolution_clock::now();

        delete[] buffer;
        std::chrono::duration<double> elapsed = end - start;
        return calculateThroughput(size, elapsed.count());
    } catch (const std::bad_alloc& e) {
        if (buffer) delete[] buffer;
        return -1.0;
    }
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_compucode_rambench_domain_BenchmarkService_benchmarkWrite(JNIEnv* env, jobject obj, jint size) {
    char* buffer = nullptr;
    try {
        buffer = new char[size];

        // Warm-up phase
        int64x2_t value_vec = {0x0101010101010101, 0x0101010101010101}; // Pattern: repeating 1s
        int size_vec = size / 16; // 16-byte chunks
        int64_t* buffer_vec = reinterpret_cast<int64_t*>(buffer);
        for (int i = 0; i < size_vec; i++) {
            vst1q_s64(buffer_vec + (i * 2), value_vec);
        }
        int remainder = size % 16;
        if (remainder > 0) {
            for (int i = size - remainder; i < size; i++) {
                buffer[i] = 1;
            }
        }
        volatile char sink_warmup = buffer[size - 1];
        (void)sink_warmup;

        // Measurement phase
        auto start = std::chrono::high_resolution_clock::now();
        for (int i = 0; i < size_vec; i++) {
            vst1q_s64(buffer_vec + (i * 2), value_vec); // Store 16 bytes
        }
        if (remainder > 0) {
            for (int i = size - remainder; i < size; i++) {
                buffer[i] = 1;
            }
        }
        volatile char sink = buffer[size - 1];
        (void)sink;
        auto end = std::chrono::high_resolution_clock::now();

        delete[] buffer;
        std::chrono::duration<double> elapsed = end - start;
        return calculateThroughput(size, elapsed.count());
    } catch (const std::bad_alloc& e) {
        if (buffer) delete[] buffer;
        return -1.0;
    }
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_compucode_rambench_domain_BenchmarkService_benchmarkCopy(JNIEnv* env, jobject obj, jint size) {
    char* src = nullptr;
    char* dst = nullptr;
    try {
        src = new char[size];
        dst = new char[size];
        memset(src, 1, size); // Initialize src with 1s

        // Warm-up phase
        int size_vec = size / 16; // 16-byte chunks
        int64_t* src_vec = reinterpret_cast<int64_t*>(src);
        int64_t* dst_vec = reinterpret_cast<int64_t*>(dst);
        for (int i = 0; i < size_vec; i++) {
            int64x2_t data = vld1q_s64(&src_vec[i * 2]);
            vst1q_s64(&dst_vec[i * 2], data);
        }
        int remainder = size % 16;
        if (remainder > 0) {
            memcpy(dst + (size - remainder), src + (size - remainder), remainder);
        }
        volatile char sink_warmup = dst[size - 1];
        (void)sink_warmup;

        // Measurement phase
        auto start = std::chrono::high_resolution_clock::now();
        for (int i = 0; i < size_vec; i++) {
            int64x2_t data = vld1q_s64(&src_vec[i * 2]); // Load 16 bytes
            vst1q_s64(&dst_vec[i * 2], data);            // Store 16 bytes
        }
        if (remainder > 0) {
            memcpy(dst + (size - remainder), src + (size - remainder), remainder);
        }
        volatile char sink = dst[size - 1];
        (void)sink;
        auto end = std::chrono::high_resolution_clock::now();

        delete[] src;
        delete[] dst;
        std::chrono::duration<double> elapsed = end - start;
        return calculateThroughput(size, elapsed.count());
    } catch (const std::bad_alloc& e) {
        if (src) delete[] src;
        if (dst) delete[] dst;
        return -1.0;
    }
}