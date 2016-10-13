#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define INITIAL_SIZE 10

void doublearr(int *arr, size_t size)
{
    int *temp = (int*) malloc(sizeof(int) * size * 2); 
    memcpy(temp, arr, sizeof(int) * size);
    free(arr);
    arr = temp;
}

void printarr(int *arr, size_t size)
{
    int i ;
    for (i = 0 ; i < size; i++)
    {
        printf("arr[%d]: %d\n", i, arr[i]); 
    } 
}

void initarr(int *arr, size_t size)
{
    int i ;
    for (i = 0 ; i < size; i++)
    {
        arr[i] = i; 
    } 
}


int main(void)
{
    size_t size = INITIAL_SIZE;
    int *arr = (int*) malloc(sizeof(int) * size); 
    printarr(arr, size);
    initarr(arr, size);
    printarr(arr, size);
    doublearr(arr, size);
    size *= 2;
    printarr(arr, size);
    
    return 0;    
}
