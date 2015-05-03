__kernel void solverMain(__global const long *offset, __global const int *input, __global char *output)
{
	const uint dimensions = get_work_dim();
	ulong threadId = get_global_id(0);
	if (dimensions >= 2) threadId += get_global_id(1) * get_global_size(0);
	if (dimensions >= 3) threadId += get_global_id(2) * get_global_size(0) * get_global_size(1);
	const ulong dataId = threadId;
	threadId += offset[0];
	threadId = threadId << 3; //*8
	char result = 0;
	const uint treeEnd = input[0] + 2;
	const uint literalCount = input[1];

	char stack[%stacksize%];
	uchar stackpos;
	
	%unroll_directive%
	for(uint j = 0; j < 8; ++j) {
		stackpos = 0;

		for(uint i = 2; i < treeEnd; ++i) {
			if (input[i] >= 0) {
				stack[stackpos] = ((threadId + j) >> (literalCount - input[i])) & 1;
				++stackpos;
			} else if (input[i] == -1) {
				stack[stackpos - 2] = stack[stackpos - 2] & stack[stackpos - 1];
				--stackpos;
			} else if (input[i] == -2) {
				stack[stackpos - 2] = stack[stackpos - 2] | stack[stackpos - 1];
				--stackpos;
			} else if (input[i] == -3) {
				stack[stackpos - 1] = !stack[stackpos - 1];
			} /*else if (input[i] == -4) {
				stack[stackpos - 2] = stack[stackpos - 2] ^ stack[stackpos - 1];
				--stackpos;
			}*/
		}
		result |= (stack[0] << j);
	}

	output[dataId] = result; 
}