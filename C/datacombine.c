#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <dirent.h>

// Author: Sung Kim.
// Read the files in folder x and write out a new single file.
// Usage: ./datacombine <input folder of cleaned files> <combined output file>.

int main(int argc, char* argv[])
{
	struct dirent *ent;
	char* dir = argv[1];
	DIR* folder = opendir(dir);
	FILE* outfile = fopen(argv[2], "w");

	char* token;
	int i;
	int maxcarid = 0;
	int maxqid = 0;
	int filecount = 0;
	int tokens[15];
	char buf[100];

	while ((ent = readdir(folder)) != NULL)
	{
		if (strcmp(ent->d_name, "..") && strcmp(ent->d_name, "."))
		{
			printf("%s\n",dir);
			strcpy(buf, dir);
			printf("%s\n",buf);
			char* rel_path = strcat(buf, ent->d_name);
			printf("rel_path: %s\n", rel_path);

			FILE* fp = fopen(rel_path, "r");
			if (fp != NULL) {
        		int curmaxcarid = 0;
        		int curmaxqid = 0;

				char line[100];
				int tokencount = 0;
				while (fgets(line, 100, fp) != NULL)
				{
				    // Tokenize the line.
					token = strtok(line, ",");

					// Convert the array of strings to ints.
					while (token != NULL)
					{
						tokens[tokencount] = atoi(token);
						token = strtok(NULL, ",");
						tokencount++;
					}
					tokencount = 0;

					if (tokens[2] > curmaxcarid)
						curmaxcarid = tokens[2];

					if (tokens[9] > curmaxqid)
						curmaxqid = tokens[9];

					// Adjust the carid's, q(uery)id's, and xway number if there is more than one file in the src dir.
					if (filecount > 0)
					{
						tokens[2] += maxcarid;
						if (tokens[0] != 0)
							tokens[9] += maxqid;
						if (tokens[0] == 0)
							tokens[4] = filecount;
					}

					// Write to the outfile.
					for (i = 0; i < sizeof(tokens)/sizeof(int) ; i++)
					{
						if (i == sizeof(tokens)/sizeof(int) - 1)
							fprintf(outfile, "%d", tokens[i]);
						else
							fprintf(outfile, "%d,", tokens[i]);
					}
					fprintf(outfile, "\n");

				}
				maxcarid += curmaxcarid+1;
				maxqid += curmaxqid+1;
			}
			fclose(fp);
			filecount += 1;
		}
	}	
	printf("%d",maxcarid-1); 
	return(0);
}

