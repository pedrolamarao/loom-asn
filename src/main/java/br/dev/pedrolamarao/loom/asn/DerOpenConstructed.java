package br.dev.pedrolamarao.loom.asn;

@SuppressWarnings("preview")
public final record DerOpenConstructed(byte type, int tag, int length) implements DerPart {

}
