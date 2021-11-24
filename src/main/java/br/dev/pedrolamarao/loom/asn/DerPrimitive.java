package br.dev.pedrolamarao.loom.asn;

import java.nio.ByteBuffer;

@SuppressWarnings("preview")
public final record DerPrimitive(byte type, int tag, ByteBuffer content) implements DerPart 
{
	public int length ()
	{
		return content().remaining();
	}
}
