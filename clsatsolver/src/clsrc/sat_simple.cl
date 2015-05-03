int evaluateVariable(long threadId, int literalId) 
{
	return (threadId >> literalId) & 1;
}

__kernel void solverMain(__global const int *input, __global char *output) 
{
	const uint dimensions = get_work_dim();
	ulong threadId = get_global_id(0);
	if (dimensions >= 2) threadId += get_global_id(1) * get_global_size(0);
	if (dimensions >= 3) threadId += get_global_id(2) * get_global_size(0) * get_global_size(1);
	const long dataId = threadId;
	const int treeEnd = input[0] + 2;
	const int literalCount = input[1];

	char stack[%stacksize%];
	uchar stackpos = 0;

	for(int i = 2; i < treeEnd; i++) {
		if (input[i] >= 0) {
			stack[stackpos] = (threadId >> (literalCount - input[i])) & 1;
			++stackpos;
		}else
		{
			if (input[i] == -1) {
				stack[stackpos - 2] = stack[stackpos - 2] && stack[stackpos - 1];
				--stackpos;
			} else if (input[i] == -2) {
				stack[stackpos - 2] = stack[stackpos - 2] || stack[stackpos - 1];
				--stackpos;
			} else if (input[i] == -3) {
				stack[stackpos - 1] = (stack[stackpos - 1] == 1 ? 0 : 1);
			} /*else if (input[i] == -4) {
				stack[stackpos - 2] = stack[stackpos - 2] ^ stack[stackpos - 1];
				--stackpos;
			}*/	
		}
	}

	output[dataId] = stack[0]; 
}