package br.dev.pedrolamarao.loom.asn;

import br.dev.pedrolamarao.loom.asn.internal.BufferSource;
import br.dev.pedrolamarao.loom.asn.internal.ChannelSource;
import br.dev.pedrolamarao.loom.asn.internal.Source;
import br.dev.pedrolamarao.loom.asn.internal.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Supplier;
import java.util.generator.GeneratorHandler;
import java.util.generator.SupplierGenerator;

public final class DerParser
{
	private static final Logger logger = LoggerFactory.getLogger(DerParser.class);
	
	private final Supplier<DerPart> generator;
	
	private int consumed = 0;
	
	private Source source;
	
	public DerParser ()
	{
		this.generator = new SupplierGenerator<DerPart>(this::run);
	}
	
	public int consumed ()
	{
		return consumed;
	}
	
	public DerPart parse (ByteBuffer source)
	{
		return parse(new BufferSource(source));
	}
	
	public DerPart parse (InputStream source)
	{
		return parse(new StreamSource(source));
	}
	
	public DerPart parse (ReadableByteChannel source)
	{
		return parse(new ChannelSource(source));
	}
	
	private DerPart parse (Source source)
	{
		this.source = source;		
		final var next = generator.get();	
		this.source = null;		
		return next;
	}
	
	public void run (GeneratorHandler<DerPart> handler)
	{
		byte byte_;
		
		while (true)
		{
			// tag
			
			final byte type = pull(handler);
			
			logger.atDebug().log("parser [{}]: type = {}", hashCode(), type);
			
			final int tag;
			
			if ((type & 0x1F) != 0x1F) {
				// low tag
				tag = (type & 0x1F);
			}
			else {
				// #TODO: high tag
				throw new RuntimeException("high tag is unsupported");
			}
			
			logger.atDebug().log("parser [{}]: tag = {}", hashCode(), tag);
			
			// length
			
			final int length;
			
			byte_ = pull(handler);
			
			if ((byte_ & 0x80) == 0) {
				// definite short length
				length = byte_;
			}
			else {
				if (byte_ != 0x80) {
					int tmp = 0;
					for (int i = 0, j = (byte_ & 0x7F); i != j; ++i) {
						tmp <<= 8;
						tmp |= (pull(handler) & 0xFF);
					}
					length = tmp;
				}
				else {
					// #TODO: indefinite length
					throw new RuntimeException("indefinite length is unsupported");
				}
			}
			
			logger.atDebug().log("parser [{}]: length = {}", hashCode(), length);
			
			// object
			
			if ((type & 0x20) == 0)
			{
				// primitive
				
				final var content = ByteBuffer.allocate(length);
				push(content, handler); // #TODO: do not copy! reuse!
				content.flip();
				
				logger.atDebug().log("parser [{}]: consumed = {}", hashCode(), consumed);
				
				handler.yield(new DerPrimitive(type, tag, content));
			}
			else
			{
				// constructed

				handler.yield(new DerOpenConstructed(type, tag, length));
				
				final var parser = new DerParser();
				
				while (parser.consumed < length)
				{
					final var part = parser.parse(source);
					handler.yield(part);
				}
				
				consumed += parser.consumed;

				handler.yield(new DerCloseConstructed(type, tag, length));
			}
		}
	}
	
	private byte pull (GeneratorHandler<?> handler)
	{
		int byte_;
		
		while ((byte_ = source.pull()) == -1) {
			handler.yield(null);
		}

		++consumed;
		return (byte) byte_;
	}
	
	private void push (ByteBuffer sink, GeneratorHandler<?> handler)
	{
		while (sink.hasRemaining()) 
		{
			final int pulled = source.push(sink);
			if (pulled == -1) {
				handler.yield(null);
			}
			else {
				consumed += pulled;
			}
		}
	}
}