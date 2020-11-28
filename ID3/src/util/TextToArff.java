package util;

import java.io.File;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.unsupervised.attribute.Remove;


/**
convert data.txt to data.arff
**/
public class TextToArff
{

	//读入txt格式数据集
	public void txtToArff(String source,String destination) throws Exception
	{
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File(source));
		String[] options = {"-F<','>"};//‘，’为分隔符
		loader.setOptions(options);
		Instances data = loader.getDataSet();
		
		/*
		 * Remove remove = new Remove(); remove.setAttributeIndices();
		 */
		
		//保存为Arff格式数据集
		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File(destination));
		saver.writeBatch();
	}
	
	public static void main(String[] args) throws Exception
	{
		TextToArff tta = new TextToArff();
		tta.txtToArff("C:\\Users\\dell\\Desktop\\data-watermelon\\test.txt", "data//test.arff");
	}

}
