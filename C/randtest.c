#include <stdio.h>
#include <time.h>
#include <stdlib.h>

int main(void)
{
    const int NORM_MAX = 100;
    int overlap = 10;
    int normalunit = RAND_MAX/NORM_MAX;
    int numtimes = 0;
    printf("RAND_MAX: %d\n", RAND_MAX);
    printf("RAND_MAX: %d\n", RAND_MAX/overlap);
    srand(time(NULL));
    int i;
    for (i = 0 ; i < 100000 ; i++)
    {
        if (rand() < normalunit * overlap) 
        {
            numtimes++;
            //printf("%d ", rand());
        }
    } 
    printf("\n");
    printf("numtimes: %d\n", numtimes);
    return 0;
}
