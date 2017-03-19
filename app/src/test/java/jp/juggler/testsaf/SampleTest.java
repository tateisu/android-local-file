package jp.juggler.testsaf;

import org.junit.Test;

import static org.junit.Assert.*;

public class SampleTest{

	@Test
	public void sample_willReturnTrue() throws Exception{
		Sample sample = new Sample();
		assertEquals( sample.willReturnTrue(), true );
	}

	@Test
	public void sample_willReturnFalse() throws Exception{
		Sample sample = new Sample();
		assertEquals( sample.willReturnFalse(), false );
	}

}