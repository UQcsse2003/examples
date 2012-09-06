package patterns.singleton;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;

public enum WindowManager {

	INSTANCE;
	
	private Set<JFrame> windows = new HashSet<JFrame>();
	
	public void registerWindow(JFrame frame) {
		windows.add(frame);
	}
	
	public void disposeAllWindows() {
		for (JFrame window : windows) {
			window.dispose();
		}
	}
	
	
}
