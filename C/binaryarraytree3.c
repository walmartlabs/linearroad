// Implement a binary tree in a heap

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "doublearr.h" // This is really a set of array utils.

#define DEBUG 0

void swap(int *arr, int i, int j);
// Return a positive integer if a swap took place.
int walkup(int *arr, int start_i, size_t curr_len);
// Return a positive integer if a swap took place.
int walkdown(int *arr, int start_i, size_t curr_len);
// Recursively check the grandparents of start_i until you find one to exchange with or simply run out of grandparents.
// Return the index of the gp with which to swap.
int check_gps_left(int *arr, int start_i, int gp_i, size_t curr_len);
int check_gps_right(int *arr, int start_i, int gp_i, size_t curr_len);
void add(int *arr, int val, size_t curr_len);
int left_or_right(int index);

// Procedure:
// 1. Add to last position
// 2. if even index: Walk up
// 3. At top, walk down
// 4. if odd and immediate parent is even: check ALL grand-parents
// 5. Do sweeps to make sure all edge leaves are correctly placed.

void swap(int *arr, int i, int j)
{
    int temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
}

// Convenience function to get the depth of a tree.
int get_depth(int curr_len)
{    
    int index = curr_len;
    int depth = 0;
    while (index > 0)
    {
        index /= 2;
        depth++;
    }  
    return depth; 
}

// Convenience method to get the width of a tree.
// For printing the tree.
int get_width(int curr_len)
{
    // Get the depth first
    int depth = 0; 
    depth = get_depth(curr_len);
    // The width will be 2^depth
    return 2 << (depth-1);  // -1 because 2^1 is already accounted for by simply 2.
}

int left_or_right(int index)
{
    int depth = 0;
    depth = get_depth(index);
    int max_i = 2 << (depth-1); // Power of 2. 
    int min_i = max_i/2; // Min for depth is Power of 2 / 2.
    int half = max_i/4;
    if (DEBUG)
        printf("depth: %d, max_i: %d, min_i: %d, half: %d\n", depth, max_i, min_i, half);
    //max_i--; // Actual max is Power of 2 - 1, but we don't need it.
    if (index < min_i + half)
        return 1; // Left
    else
        return 2; // Right
}

// The start_i will be the curr_len-1.
int walkup(int *arr, int start_i, size_t curr_len)
{
    int return_val = 0;
    // Check the parent. If left/even and less than parent, OK. If right/odd and < parent, not OK. If at root (i==1), stop.
    if (start_i <= 1) // You're at the root, stop walking down.
    {
        return_val += walkdown(arr, 1, curr_len);
    } 
    // EVEN-LEFT > P WALKUP: 
    else if (start_i % 2 == 0 && arr[start_i] > arr[start_i/2]) 
    {
        if (DEBUG)
            printf("walkup left:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
        swap(arr, start_i, start_i/2);
        return_val++;
        if (DEBUG)
            printf("walkup left:arr[%d], arr[%d], %d, %d\n", start_i/2, start_i, arr[start_i/2], arr[start_i]); // DB
        return_val += walkup(arr, start_i/2, curr_len);
    }
    // ODD-WALKUP P -> GP -> GGP -> etc.: 
    else if (start_i % 2 == 1 && (start_i / 2) % 2 == 0) // And the immediate parent is EVEN.
    { 
        int gp_swap_i = check_gps_left(arr, start_i, start_i / 4, curr_len);    
        if (gp_swap_i > 0)
        {
            if (DEBUG)
                printf("walkup left 2 gp:arr[%d], arr[%d], %d, %d\n", gp_swap_i, start_i, arr[gp_swap_i], arr[start_i]); // DB
            swap(arr, start_i, gp_swap_i);
            return_val++;
            if (DEBUG)
                printf("walkup left 2 gp:arr[%d], arr[%d], %d, %d\n", gp_swap_i, start_i, arr[gp_swap_i], arr[start_i]); // DB
            return_val += walkdown(arr, gp_swap_i, curr_len);
        } 
    }
    return_val += walkdown(arr, start_i, curr_len);
    return return_val;
}

int walkdown(int *arr, int start_i, size_t curr_len)
{
    int return_val = 0; 
    // Check size, or ability to walkdown.
    if (start_i*2 > curr_len) 
        return return_val;
    // Check each child and swap if necessary.
    // Check left child.
    if (arr[start_i] < arr[start_i*2])
    {
        if (DEBUG)
            printf("walkdown:arr[%d], arr[%d], %d, %d\n", start_i, start_i*2, arr[start_i], arr[start_i*2]); // DB
        swap(arr, start_i, start_i*2); 
        return_val++;
        if (DEBUG)
            printf("walkdown:arr[%d], arr[%d], %d, %d\n", start_i, start_i*2, arr[start_i], arr[start_i*2]); // DB
        return_val += walkdown(arr, start_i*2, curr_len);
    }
    // Check size before trying left
    if (start_i*2+1 > curr_len)
        return return_val;
    if (arr[start_i] > arr[start_i*2+1])
    {
        if (DEBUG)
            printf("walkdown:arr[%d], arr[%d], %d, %d\n", start_i, start_i*2+1, arr[start_i], arr[start_i*2+1]); // DB
        swap(arr, start_i, start_i*2+1); 
        return_val++;
        if (DEBUG)
            printf("walkdown:arr[%d], arr[%d], %d, %d\n", start_i, start_i*2+1, arr[start_i], arr[start_i*2+1]); // DB
        return_val += walkdown(arr, start_i*2+1, curr_len);
    } 
    return return_val;
    // Check right child. 
    // Stop if you can't swap
}

int check_gps_left(int *arr, int start_i, int gp_i, size_t curr_len)
{
    if (DEBUG)
        printf("check_gps_left start_i:%d = %d,  gp_i:%d = %d\n", start_i, arr[start_i], gp_i, arr[gp_i]);
    if (gp_i < 1)
        return -1;
    if (arr[start_i] > arr[gp_i])
    {
        if (DEBUG)
            printf("check_gps_left FOUND start_i:%d = %d,  gp_i:%d = %d\n", start_i, arr[start_i], gp_i, arr[gp_i]);
        return gp_i;
    }
    else
     //   return check_gps_left(arr, start_i, gp_i/2, curr_len);
    return -1;
}

int check_gps_right(int *arr, int start_i, int gp_i, size_t curr_len)
{
    if (DEBUG)
        printf("check_gps_right start_i:%d = %d,  gp_i:%d = %d\n", start_i, arr[start_i], gp_i, arr[gp_i]);
    if (gp_i < 1)
        return -1;
    if (arr[start_i] < arr[gp_i])
    {
        if (DEBUG)
            printf("check_gps_right FOUND start_i:%d = %d,  gp_i:%d = %d\n", start_i, arr[start_i], gp_i, arr[gp_i]);
        return gp_i;
    }
    //else
     //   return check_gps_right(arr, start_i, gp_i/2, curr_len);
    return -1;
}

// curr_len is the current length (i.e. filled values) of the array.
// Any necessary resizing should occur before we attempt to add an element.
// We don't care about the value at arr[0]. We could make it hold the size.
void add(int *arr, int val, size_t curr_len)
{
    arr[curr_len+1] = val;    
    walkup(arr, curr_len+1, curr_len); 

    // Need a sweep of edge leaves.
    // And we need to sweep until no walk ups can take place.
    if (DEBUG)
        printf("======\nBegin Sweeping\n======\n");
    int num_sweeps = 1;
    int keep_sweeping = 1;
    int i;
    while(keep_sweeping)
    {
        if (DEBUG)
            printf("Sweep number: %d\n", num_sweeps);
        for (i = curr_len+1 ; i > (curr_len+1)/2 ; i--)
        {
            keep_sweeping = walkup(arr, i, curr_len);
            // Check if on left and greater than root.
            if (left_or_right(i) == 1) // Left
            {
                if (arr[i] > arr[1])
                {
                    swap(arr, i, 1);
                    walkup(arr, 1, curr_len);
                    keep_sweeping++;
                }
            } 
            else // Right
            {
                if (arr[i] < arr[1])
                {
                    swap(arr, i, 1);
                    walkup(arr, 1, curr_len);
                    keep_sweeping++;
                }
            }
        } 
        num_sweeps++;
    }
   
    if (DEBUG)     
        printf("======\nPlaced %d\n======\n", val);
}

// Walk the array to see if value exists.
int find(int *arr, int val, size_t curr_len)
{
    int i = 1;
    while(i <= curr_len + 1)
    { 
        if(val == arr[i])
            return i; 
        else if(val < arr[i])
            i = i * 2;
        else if(val > arr[i])
            i = i * 2 + 1;
    }
    return -1;
}

void printbinarytree(int *arr, size_t curr_len)
{
    // Need to indent root up to 2 * depth
    //        1
    //     -     -   
    //    0       2

    int depth = 0;
    int width = 0;
    depth = get_depth(curr_len);
    width = get_width(curr_len);
    // When we print we need enough space to print all, COMPLETE, sub-trees. 
    // Each leave takes 5, so width * 5.
    // Our initial padding is thus 5 * width.
    if (DEBUG)
        printf("printbinarytree():curr_len:%d\n", curr_len);
    // Indent 2 * depth for root
    int padding = 0;
    int padding_between = 0; // The padding between numbers on the same row.
    //padding += 2 * depth;
    padding += 2 * width;
    int curr_depth = 0;
    int i, j, k;
    i = 1; // We start our walk from index 1.
    while (1)
    {
        // Print the padding initial.
        for (j = 0 ; j < padding ; j++) printf(" ");    
        // For even indices, print lines after the value to align under the parent. 
        if (i % 2 == 0)
        {
            // Print the val.
            printf("%d", arr[i++]);
            for (j = 0 ; j < padding_between-1 ; j++) printf("-");    
            printf("/");
        }
        // Print the padding between if any.
        //for (j = 0 ; j < padding_between ; j++) printf(" ");    
        // For odd, print lines before the value to align with parent.
        if (i % 2 == 1) //&& (i > 1))
        {
            if (i > 1) printf("\\");
            for (j = 0 ; j < padding_between-1 ; j++) printf("-");    
            // Print the val.
            printf("%d", arr[i++]);
            // Add padding aftewards
            if (i > 1) for (j = 0 ; j < padding_between ; j++) printf(" ");    
        }
       // Print the lines if any. 
        if (i < curr_len)
        {    
            if(get_depth(i) > curr_depth)
            {
                //if (DEBUG)
                    //printf("Depth increased from %d to %d\n", curr_depth, get_depth(i));
                curr_depth = get_depth(i); // Move down a level.
                // Create the correct padding between for this depth.
                padding_between = padding/2 - 1 ; 
                // Decrease the initial padding by 1/2 each level. 
                padding /= 2;
                printf("\n");
            } 
            //if (DEBUG)
                //printf("printbinarytree():curr_depth:%d\n", curr_depth); 
        }
        else
        {
            printf("\n");
            return;
        }
    }
}
    

// Test
int main(int argc, char* argv[])
{
    if (argc != 2)
    {
        printf("Usage: ./binaryarraytree3 <int>\n");
        return 1;
    }   
    size_t init_size = atoi(argv[1]);
    init_size++; // Because we are going to be 1-indexed.
    size_t curr_len = 0;

    int *arr = (int*) malloc(sizeof(int) * init_size);
    initarr_withval(arr, init_size, -1);
    if (DEBUG)
        printarr_optnewline(arr, init_size, 0); // We need to +1 to curr_len to print out the last array member since we're starting from index 1.    

    // Do some initial adding.
    int i;
    for (i = 0 ; i < init_size ; i++)
    {
        if (DEBUG)
            printf("======\nAdding %d\n======\n", i);
        if (curr_len > init_size) // Expand the array 
        {
            printf("Doubling the size of arr.\n");
            doublearr_withval(arr, curr_len, -1);            
        }
        add(arr, i, curr_len);
        curr_len++;
    } 
    
    printarr(arr, curr_len + 1); // We need to +1 to curr_len to print out the last array member since we're starting from index 1.    

    printbinarytree(arr, curr_len);

    // Test finding
    printf("Find 0: %d\n", find(arr, 0, curr_len));
    printf("Find 5: %d\n", find(arr, 5, curr_len));
    printf("Find 9: %d\n", find(arr, 9, curr_len));
    printf("Find 20: %d\n", find(arr, 20, curr_len));
    printf("Find 25: %d\n", find(arr, 25, curr_len));
    for (i = 0 ; i < curr_len + 1 ; i++)
    {
        printf("Find %d: %d\n", i, find(arr, i, curr_len));
    }

    //free(arr);

    return 0;
}
