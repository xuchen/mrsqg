/**
 *
 */
package com.googlecode.mrsqg.mrs;

import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * @author Xuchen Yao
 *
 */
public class DMRS {

	private static Logger log = Logger.getLogger(DMRS.class);

	public static enum PRE_SLASH {
		NULL, RSTR, ARG, L_INDEX, R_INDEX, L_HNDL, R_HNDL, OTHER, NOTHING
	}

	public static enum POST_SLASH {
		NULL, H, EQ, NEQ, HEQ, OTHER, NOTHING
	}

	public enum DIRECTION {
		NULL, GOV, DEP
	}

	public static final HashMap<String, PRE_SLASH> preSlashMap = new HashMap<String, PRE_SLASH>() {
		{
			put("RSTR", PRE_SLASH.RSTR);
			put("ARG", PRE_SLASH.ARG);
			put("L-INDEX", PRE_SLASH.L_INDEX);
			put("R-INDEX", PRE_SLASH.R_INDEX);
			put("L-HNDL", PRE_SLASH.L_HNDL);
			put("R-HNDL", PRE_SLASH.R_HNDL);
		}
	};

	protected PRE_SLASH preSlash;
	protected POST_SLASH postSlash;
	protected DIRECTION direction;
	/*
	 * If preSlash == ARG, <code>arg<code> encodes which argument (i.e. 0, 1, 2, ""...).
	 * In a very rare case, unknown_rel takes ARG and ARG0. see core.smi from erg.
	 */
	protected String arg;

	EP ep;

	public PRE_SLASH getPreSlash() { return preSlash;}
	public POST_SLASH getPostSlash() { return postSlash;}
	public DIRECTION getDirection() { return direction;}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String value = null;
		sb.append(" ");
		if (direction == DIRECTION.GOV)
			sb.append("<-");
		else
			sb.append("--");
		if (this.preSlash == PRE_SLASH.ARG) {
			sb.append("ARG"+arg+"/"+postSlash.toString());
		} else {
			sb.append(preSlash.toString()+"/"+postSlash.toString());
		}
		if (direction == DIRECTION.DEP)
			sb.append("-> ");
		else
			sb.append("-- ");
		if (ep == null)
			sb.append("null");
		else {
			sb.append(ep.getTypeName());
			value = ep.getValueByFeature("CARG");
		}
		if (value != null) {
			sb.append("("+value+")");
		}

		return sb.toString();
	}

	public DMRS (EP ep, PRE_SLASH pre, POST_SLASH post, DIRECTION dir) {
		this.ep = ep;
		this.preSlash = pre;
		this.postSlash = post;
		this.direction = dir;
	}

	public DMRS (EP ep, PRE_SLASH pre, POST_SLASH post, DIRECTION dir, String arg) {
		this(ep, pre, post, dir);
		this.arg = arg;
	}

	public EP getEP() {return ep;}

	public String getArgNum() {return arg;}

	public boolean isPreArg2() {return preSlash==PRE_SLASH.ARG && arg.equals("2");}

	public static PRE_SLASH mapPreSlash (String preSlash) {
		PRE_SLASH p = preSlashMap.get(preSlash.toUpperCase());
		if (p!=null)
			return p;
		else {
			log.error("preSlash feature is unknown! "+preSlash);
			return PRE_SLASH.OTHER;
		}
	}


}
