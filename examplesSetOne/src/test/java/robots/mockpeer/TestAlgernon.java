package robots.mockpeer;

import robocode.ScannedRobotEvent;
import robocode.robotinterfaces.peer.IAdvancedRobotPeer;

import org.junit.*;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

public class TestAlgernon {
	
	private AlgernonReborn alg = new AlgernonReborn(); 
	private IAdvancedRobotPeer mockPeer = Mockito.mock(IAdvancedRobotPeer.class);
	
	@Before
	public void before() {
		alg.setPeer(mockPeer);
	}
	
	@Test 
	public void testFires() {
		
		ScannedRobotEvent mockEvent = Mockito.mock(ScannedRobotEvent.class);
		when(mockEvent.getDistance()).thenReturn(2d);
		when(mockEvent.getBearing()).thenReturn(0d);
		when(mockEvent.getVelocity()).thenReturn(0d);
		when(mockEvent.getHeading()).thenReturn(0d);
		when(mockEvent.getEnergy()).thenReturn(10d);
		when(mockEvent.getName()).thenReturn("testRobot");
		
	
		alg.onScannedRobot(mockEvent);
		alg.doTurnActions();
		
		verify(mockPeer).fire(3d);		
		
	}	
	
}
