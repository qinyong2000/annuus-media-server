package com.ams.util;

public final class Utils {

	public static int from16Bit(byte[] b) {
		return (((b[0] & 0xFF) << 8) | (b[1] & 0xFF)) & 0xFFFF;
	}

	public static int from24Bit(byte[] b) {
		return (((b[0] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[2] & 0xFF)) & 0xFFFFFF;
	}

	public static long from32Bit(byte[] b) {
		return (((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
				| ((b[2] & 0xFF) << 8) | (b[3] & 0xFF)) & 0xFFFFFFFFL;
	}

	public static int from16BitLittleEndian(byte[] b) {
		// 16 Bit read, LITTLE-ENDIAN
		return (((b[1] & 0xFF) << 8) | (b[0] & 0xFF)) & 0xFFFF;
	}

	public static int from24BitLittleEndian(byte[] b) {
		// 24 Bit read, LITTLE-ENDIAN
		return (((b[2] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[0] & 0xFF)) & 0xFFFFFF;
	}

	public static long from32BitLittleEndian(byte[] b) {
		// 32 Bit read, LITTLE-ENDIAN
		return (((b[3] & 0xFF) << 24) | ((b[2] & 0xFF) << 16)
				| ((b[1] & 0xFF) << 8) | (b[0] & 0xFF)) & 0xFFFFFFFFL;
	}
	
	public static byte[] to16Bit(int v) {
		byte[] b = new byte[2];
		b[1] = (byte) (v & 0xFF);
		b[0] = (byte) ((v & 0xFF00) >>> 8);
		return b;
	}

	public static byte[] to24Bit(int v) {
		byte[] b = new byte[3];
		b[2] = (byte) (v & 0xFF);
		b[1] = (byte) ((v & 0xFF00) >>> 8);
		b[0] = (byte) ((v & 0xFF0000) >>> 16);
		return b;
	}

	public static byte[] to32Bit(long v) {
		byte[] b = new byte[4];
		b[3] = (byte) (v & 0xFF);
		b[2] = (byte) ((v & 0xFF00) >>> 8);
		b[1] = (byte) ((v & 0xFF0000) >>> 16);
		b[0] = (byte) ((v & 0xFF000000) >>> 24);
		return b;
	}

	public static byte[] to16BitLittleEndian(int v) {
		// 16bit write, LITTLE-ENDIAN
		byte[] b = new byte[2];
		b[0] = (byte) (v & 0xFF);
		b[1] = (byte) ((v & 0xFF00) >>> 8);
		return b;
	}

	public static byte[] to24BitLittleEndian(int v) {
		byte[] b = new byte[3];
		b[0] = (byte) (v & 0xFF);
		b[1] = (byte) ((v & 0xFF00) >>> 8);
		b[2] = (byte) ((v & 0xFF0000) >>> 16);
		return b;
	}

	public static byte[] to32BitLittleEndian(long v) {
		byte[] b = new byte[4];
		// 32bit write, LITTLE-ENDIAN
		b[0] = (byte) (v & 0xFF);
		b[1] = (byte) ((v & 0xFF00) >>> 8);
		b[2] = (byte) ((v & 0xFF0000) >>> 16);
		b[3] = (byte) ((v & 0xFF000000) >>> 24);
		return b;
	}
}
