package se.sll.reimbursementadapter.admincareevent.transformer;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import se.sll.ersmo.xml.indata.ERSMOIndata;

public class ERSMOIndataUnMarshaller extends AbstractTransformer {
	
	public ERSMOIndataUnMarshaller() {
		 registerSourceType(DataTypeFactory.STRING);
	     setReturnDataType(DataTypeFactory.create(ERSMOIndata.class));
	     setName("ERSMOIndataUnMarshaller");
	}

	@Override
	protected Object doTransform(Object src, String enc)
			throws TransformerException {
		String fileContent = (String) src;
		ERSMOIndata indata = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(ERSMOIndata.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			StringReader reader = new StringReader(fileContent);
			indata = (ERSMOIndata) unmarshaller.unmarshal(reader);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return indata;
	}

}
