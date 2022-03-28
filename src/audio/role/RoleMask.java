package audio.role;

import java.util.BitSet;

public class RoleMask {
	
	private final BitSet mask;
	
	public static RoleMask ofRolesSelf(Role... roles) {
		BitSet mask = new BitSet();
		for(Role role:roles) {
			role.populateSelf(mask);		
		}
		return new RoleMask(mask);
	}
	
	public static RoleMask ofRolesRecursive(Role... roles) {
		BitSet mask = new BitSet();
		for(Role role:roles) {
			role.populateRecursive(mask);		
		}
		return new RoleMask(mask);
	}
	
	private RoleMask(BitSet mask) {
		this.mask = mask;
	}
	
	public void checkIntersects(BitSet roleBits) {
		if(roleBits == null) {
			throw new RuntimeException("Could not check role because roleBits not available.");
		}
		if(!mask.intersects(roleBits)) {
			throw new RuntimeException("No matching roles in account");
		}
	}
}
