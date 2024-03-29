importClass(org.ow2.proactive.resourcemanager.utils.console.ResourceManagerModel);

var rm_ = ResourceManagerModel.getModel(false);
var rm = rm_.getResourceManager();

function filterspush(regexp){
	rm_.filtersPush_(regexp);
}
function filterspop(){
	return rm_.filtersPop_();
}
function filtersclear(){
	rm_.filtersClear_();
}
function setpagination(state){
	if (state){
		rm_.setPagination_(true);
	} else {
		rm_.setPagination_(false);
	}
}

function addcandidate(str){
	if (str == undefined || str == ""){
		str = null;
	}
	rm_.addCandidate_(str);
}

function exmode(displayStack, displayOnDemand){
	if (displayStack == undefined){
		displayStack = true;
	}
	if (displayOnDemand == undefined){
		displayOnDemand = true;
	}
	rm_.setExceptionMode_(displayStack, displayOnDemand);
}

function addnode(nodeURL, nodeSourceName){
	if (nodeSourceName==undefined){
		nodeSourceName = null;
		println("Node Source will be the default one as it is not set");
	}
	return rm_.addnode_(nodeURL, nodeSourceName);
}

function removenode(nodeURL,preemptively){
	if (preemptively == undefined){
		preemptively = false;
		println("Preemptive mode will be false as it is not set");
	}
    return rm_.removenode_(nodeURL,preemptively);
}

function createns(nsName,infrastructure,policy){
	if (infrastructure == undefined){
		infrastructure = null;
		println("Infrastructure will be the default one as it is not set");
	}
	if (policy == undefined){
		policy = null;
		println("Policy will be the default one as it is not set");
	}
	return rm_.createns_(nsName,infrastructure,policy);
}

function removens(nodeSourceName,preemptively){
	if (preemptively == undefined){
		preemptively = false;
		println("Preemptive mode will be false as it is not set");
	}
	return rm_.removens_(nodeSourceName,preemptively);
}

function locknode(nodeURL){
    var list = java.util.Collections.singletonList(nodeURL);
	return rm_.locknodes_(new java.util.HashSet(list));
}

function unlocknode(nodeURL){
    var list = java.util.Collections.singletonList(nodeURL);
	return rm_.unlocknodes_(new java.util.HashSet(list));
}

function listnodes(nodeSourceName){
	if (nodeSourceName == undefined) {
		nodeSourceName = null;
	}
    return rm_.listnodes_(nodeSourceName);
}

function topology(){
    return rm_.topology_();
}

function listns(){
    return rm_.listns_();
}

function listinfrastructures(){
	return rm_.listInfrastructures_();
}

function listpolicies(){
	return rm_.listPolicies_();
}

function shutdown(preemptively){
	if (preemptively == undefined){
		preemptively = false;
		println("Preemptive mode will be false as it is not set");
	}
	return rm_.shutdown_(preemptively);
}

function stats(){
	rm_.showRuntimeData_();
}

function myaccount(){
	rm_.showMyAccount_();
}

function account(username){
	rm_.showAccount_(username);
}

function nodeinfo(nodeURL){
	rm_.nodeinfo_(nodeURL);
}

function reloadconfig(){
	rm_.refreshConfiguration_();
}

function reconnect(rmURL){
	if (rmURL == undefined){
		rmURL = null;
	}
	rm_.reconnect_();
}

function exec(commandFilePath){
	return rm_.exec_(commandFilePath);
}

function exit(){
	return rm_.exit_();
}

function cnslhelp(){
	rm_.cnslhelp_();
}

function help(){
	rm_.help_();
}
