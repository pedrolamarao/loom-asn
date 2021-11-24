package br.dev.pedrolamarao.loom.asn;

@SuppressWarnings("preview")
public final record DerCloseConstructed(byte type, int tag, int length) implements DerPart {

}
