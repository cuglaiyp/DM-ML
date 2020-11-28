package com;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;

public class Id3Test {
	// 读取数据集
	public Instances getInstances(String fileName) throws Exception {
		Instances m_instances = DataSource.read(fileName);
		m_instances.setClassIndex(m_instances.numAttributes() - 1);
		return m_instances;
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String name = "data/train-big.arff";
		Id3Test id3Test = new Id3Test();
		Instances instances = id3Test.getInstances(name);
		MyId3 classifier = new MyId3();
		classifier.buildClassifier(instances);

		//System.out.println(classifier.toString());

		// 获取测试用例
		Instances testIns = id3Test.getInstances("data/test-big.arff");
		Evaluation eval = new Evaluation(testIns);
		eval.evaluateModel(classifier, testIns);
		System.out.println(eval.toClassDetailsString());
		System.out.println(eval.toSummaryString());
		System.out.println(eval.toMatrixString());

		// 决策树可视化

		TreeVisualizer tv = new TreeVisualizer(null, classifier.graph(), new PlaceNode2());
		JFrame jf = new JFrame("weka Classifier Tree Visualizer:ID3");
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jf.setSize(700, 700);
		jf.getContentPane().setLayout(new BorderLayout());
		jf.getContentPane().add(tv, BorderLayout.CENTER);
		jf.setVisible(true);
		tv.fitToScreen();
	}

}
