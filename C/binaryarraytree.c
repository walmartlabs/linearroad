// Implement a binary tree in a heap

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "doublearr.h"

// For a binary tree implemented in an array:
// Start with index 1 as the root,
// Then the leaves of the root are (and these are all indices):
// Left, Right:    1 + (*2, *2+1) => 2,3
// Then the leaves of 2 and 3 are:
// Left, Right:    2 + (*2, *2+1) => 4,5 
// Left, Right:    3 + (*2, *2+1) => 6,7
// Then the leaves of 4 and 5 are:
// 4 + (*2, *2+1) => 8,9
// 5 + (*2, *2+1) => 10, 11  
// Then the leaves of 4 and 5 are:
// 6 + (*2, *2+1) => 12,13
// 7 + (*2, *2+1) => 14,15 // ...

// Odds are right, evens are left.
// Going up is simply i/2.

void swap(int *arr, int i, int j)
{
    int temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
}

// The start_i will be the curr_len-1.
void walkup(int *arr, int start_i)
{
    // Check the parent. If left/even and less than parent, OK. If right/odd and < parent, not OK. If at root (i==1), stop.
    if (start_i <= 1) // You're at the root, stop walking up.
    {
        return;
    } 
    // If on the left(even index) and greater than parent...
    else if (start_i % 2 == 0 && arr[start_i] > arr[start_i/2]) 
    {
        printf("walkup:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
        swap(arr, start_i, start_i/2);
        printf("walkup:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
        walkup(arr, start_i/2);
        printf("done with: walkup:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
    }
    // If on the right(odd index) on a grand-child on the right of a grand-parent and greater than grand-parent ...
    else if (start_i % 2 == 1 && (start_i / 2) % 2 == 0  && arr[start_i] > arr[start_i/4])
    { 
        printf("walkup:arr[%d], arr[%d], %d, %d\n", start_i/4, start_i, arr[start_i/4], arr[start_i]); // DB
        swap(arr, start_i, start_i/4);
        // Check if grand-parent is greater than its right child
        if (arr[start_i / 4] > arr[start_i / 2]) 
        {
            swap(arr, start_i / 2, start_i / 4);           
            printf("walkup (gp with i):arr[%d], arr[%d], %d, %d\n", start_i/4, start_i, arr[start_i/4], arr[start_i]); // DB
            // \\Now swap i with the new grand-parent.
            // Now swap the former grand-parent  with the smallest value on the right side 
            // This works because this will be put into the last spot used spot and will less than the new grand-parent. 
            // We may have to change from grad-parent to 'root'.
            //\\swap(arr, start_i, start_i / 4);
            // We follow with a: start_i / 2 + start_i % (left-most-right, which is the smallest value on the right, which is the smallest index
            // 0)
            int leftmost = start_i / 2 + (int)floor(sqrt(start_i));
            swap(arr, start_i, leftmost); 
            printf("final: walkup:arr[%d], arr[%d], %d, %d\n", leftmost, start_i, arr[leftmost], arr[start_i]); // DB
        }
    }
    // Once at root, check right child and walk down. 
        // If you swap, make sure the left child is still proper.
}

void walkdown(int *arr, int start_i)
{
    // Check the parent. If left/even and less than parent, OK. If right/odd and < parent, not OK. If at root (i==1), stop.
    if (start_i <= 1) // You're at the root, stop walking up.
    {
        return;
    } 
    else if (arr[start_i] > arr[start_i/2])
    {
        printf("walkup:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
        swap(arr, start_i, start_i/2);
        printf("walkup:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
        walkup(arr, start_i/2);
        printf("done with: walkup:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
    }
    // Once at root, check right child and walk down. 
        // If you swap, make sure the left child is still proper.
}

// curr_max is the current length (i.e. filled values) of the array.
// Any necessary resizing should occur before we attempt to add an element.
// We don't care about the value at arr[0]. We could make it hold the size.
void add(int *arr, int val, size_t curr_max)
{
    arr[curr_max+1] = val;    
    walkup(arr, curr_max+1); 
}

int find(int *arr, int val, size_t size)
{
    return -1;
}

int main(int argc, char* argv[])
{
    size_t init_size = atoi(argv[1]);
    size_t curr_len = 0;

    int *arr = (int*) malloc(sizeof(int) * init_size);
    initarr_withval(arr, init_size, -1);

    // Do some initial adding.
    int i;
    //for (i = 0 ; i < init_size*2 + (init_size/2) ; i++)
    for (i = 0 ; i < init_size ; i++)
    {
        if (curr_len >= init_size-1) // Expand the array 
        {
            printf("Doubling the size of arr.\n");
            doublearr_withval(arr, curr_len, -1);            
        }
        add(arr, i, curr_len);
        curr_len++;
    } 
    
    printarr(arr, curr_len + 1); // We need to +1 to curr_len to print out the last array member since we're starting from index 1.    

    //free(arr);

    return 0;
}
