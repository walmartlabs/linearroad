#ifndef DOUBLEARR_H
#define DOUBLEARR_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define INITIAL_SIZE 10

void doublearr(int *arr, size_t size);
void doublearr_withval(int *arr, size_t size, int defaultval);
void printarr(int *arr, size_t size);
void printarr_optnewline(int *arr, size_t size, int newline);
void initarr(int *arr, size_t size);
void initarr_withval(int *arr, size_t size, int defaultval);
void initarr_withval_withrange(int *arr, size_t size, int start_i, int end_i, int defaultval);

// Simply double an array, copying existing elements.
void doublearr(int *arr, size_t size)
{
    int *temp = (int*) malloc(sizeof(int) * size * 2); 
    memcpy(temp, arr, sizeof(int) * size);
    free(arr);
    arr = temp;
}

// Double an array, but put a default value for the newly allocated portions.
void doublearr_withval(int *arr, size_t size, int defaultval)
{
    int *temp = (int*) malloc(sizeof(int) * size * 2); 
    initarr_withval(temp, size * 2, defaultval); // This wastes
    memcpy(temp, arr, sizeof(int) * size);
    free(arr);
    arr = temp;
}

// Print an array with newlines.
void printarr(int *arr, size_t size)
{
    int i ;
    for (i = 0 ; i < size; i++)
    {
        printf("arr[%d]: %d\n", i, arr[i]); 
    } 
}

// Print an array with or without newlines.
// Use int newline as the pseudo-boolean.
void printarr_optnewline(int *arr, size_t size, int newline)
{
    int i ;
    for (i = 0 ; i < size; i++)
    {
        if (newline)
            printf("arr[%d]: %d\n", i, arr[i]); 
        else
            printf("arr[%d]:%d ", i, arr[i]);  // We intentionally leave out the space after 'arr[i]' for non-newline printing.
    } 
    printf("\n");
}

// Simple array initialization with value of the index.
void initarr(int *arr, size_t size)
{
    int i ;
    for (i = 0 ; i < size; i++)
    {
        arr[i] = i; 
    } 
}

// Simple array initialization with a default value.
void initarr_withval(int *arr, size_t size, int defaultval)
{
    int i ;
    for (i = 0 ; i < size ; i++)
    {
        arr[i] = defaultval; 
    } 
}

// Simple array initialization with a default value and within a range.
// If end_i > size end_i defaults to size.
// [start_i, end_i), exclusive of end_i.
void initarr_withval_withrange(int *arr, size_t size, int start_i, int end_i, int defaultval)
{
    // Make sure the end range is less than the size (or better to throw an error?)
    if (end_i > size)
        end_i = size;
    int i ;
    for (i = start_i ; i < end_i; i++)
    {
        arr[i] = defaultval; 
    } 
}




/*
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
*/

#endif /* DOUBLEARR_H */
