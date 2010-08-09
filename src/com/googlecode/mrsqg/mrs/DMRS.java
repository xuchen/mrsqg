/**
 *
 */
package com.googlecode.mrsqg.mrs;

/**
 * @author Xuchen Yao
 *
 */
public class DMRS {

	public enum PRE_SLASH {
		NULL, RSTR, ARG, L_INDEX, R_INDEX, L_HNDL, R_HNDL, OTHER
	}

	public enum POST_SLASH {
		NULL, H, EQ, NEQ, HEQ, OTHER
	}

	public enum DIR {
		NULL, GOV, DEP
	}

	protected PRE_SLASH preSlash;
	protected POST_SLASH postSlash;
	/*
	 * If preSlash == ARG, <code>arg<code> encodes which argument (i.e. 0, 1, 2...).
	 */
	protected int arg;

	ElementaryPredication ep;

	public DMRS (ElementaryPredication ep, PRE_SLASH pre, POST_SLASH post) {
		this.ep = ep;
		this.preSlash = pre;
		this.postSlash = post;
	}

	public DMRS (ElementaryPredication ep, PRE_SLASH pre, POST_SLASH post, int arg) {
		this(ep, pre, post);
		this.arg = arg;
	}

}
