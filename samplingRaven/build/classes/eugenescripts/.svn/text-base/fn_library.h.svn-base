//Returns a subdevice of a specific device given some part indices
function Device subDevice(Device d, num start, num end) {
	Device build();
	for (num i = start; i < end; i++) {
		build.add(d.get(i));
	}
	return build;
}

//Returns list of Devices that are intermediates of another device
function Device[] getInts(Device d) {	
	
	//Initialize intermediates list
	num dSize = d.size();
	Device[] intermediates;
	
	//Return each intermediate to a device for evaluation
	for (num i = 0; i < dSize; i++) {
		for (num j = i + 2; j < (dSize + 1); j++) {
			Device int = subDevice(d,i,j);
			intermediates.add(int);
		}
	}
	return intermediates;
}