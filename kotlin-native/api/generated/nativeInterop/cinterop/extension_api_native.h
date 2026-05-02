#include <stdint.h>
#include <stdbool.h>

typedef struct {
    float left;
    float right;
} AudioFrame;

typedef struct {
    uint64_t id;        /* default: 0 */
} ObjectID;
