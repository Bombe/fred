package freenet.node.useralerts;

import freenet.support.HTMLNode;

public class ExtOldAgeUserAlert implements UserAlert {
	private boolean isValid=true;
	
	public boolean userCanDismiss() {
		return true;
	}

	public String getTitle() {
		return "Freenet-ext too old";
	}
	
	public String getText() {
		String s;
		s = "Your freenet-ext.jar file seems to be outdated : we strongly advise you to update it using http://downloads.freenetproject.org/alpha/freenet-ext.jar.";
		return s;
	}

	public HTMLNode getHTMLText() {
		return new HTMLNode("div", "Your freenet-ext.jar file seems to be outdated: we strongly advise you to update it using http://downloads.freenetproject.org/alpha/freenet-ext.jar.");
	}

	public short getPriorityClass() {
		return UserAlert.ERROR;
	}

	public boolean isValid() {
		return isValid;
	}
	
	public void isValid(boolean b){
		if(userCanDismiss()) isValid=b;
	}
	
	public String dismissButtonText(){
		return "Hide";
	}
	
	public boolean shouldUnregisterOnDismiss() {
		return true;
	}
	
	public void onDismiss() {
		// do nothing on alert dismissal
	}
}
