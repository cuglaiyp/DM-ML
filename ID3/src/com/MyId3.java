package com;
/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    Id3.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Sourcable;
import weka.classifiers.rules.OneR;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NoSupportForMissingValuesException;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Map;

/**
 * <!-- globalinfo-start --> Class for constructing an unpruned decision tree
 * based on the ID3 algorithm. Can only deal with nominal attributes. No missing
 * values allowed. Empty leaves may result in unclassified instances. For more
 * information see: <br/>
 * <br/>
 * R. Quinlan (1986). Induction of decision trees. Machine Learning.
 * 1(1):81-106.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Quinlan1986,
 *    author = {R. Quinlan},
 *    journal = {Machine Learning},
 *    number = {1},
 *    pages = {81-106},
 *    title = {Induction of decision trees},
 *    volume = {1},
 *    year = {1986}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 *
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 *  -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8109 $
 */
public class MyId3 extends AbstractClassifier implements TechnicalInformationHandler, Sourcable, Drawable {

	private Map<String, Double> weights;

	/** for serialization */
	static final long serialVersionUID = -2693678647096322561L;
	/** the node's id */
	private int m_id;

	/** static count to assign the ids */
	private static int ms_count = 0;
	/** The node's successors. */
	private MyId3[] m_Successors;

	/** Attribute used for splitting. */
	private Attribute m_Attribute;

	/** Class value if node is leaf. */
	private double m_ClassValue;

	/** Class distribution if node is leaf. */
	private double[] m_Distribution;

	/** Class attribute of dataset. */
	private Attribute m_ClassAttribute;

	public MyId3() {
		// Assign ids to each node of the id3 tree
		m_id = ms_count++;
	}

	/**
	 * Returns a string describing the classifier.
	 * 
	 * @return a description suitable for the GUI.
	 */
	public String globalInfo() {

		return "Class for constructing an unpruned decision tree based on the ID3 "
				+ "algorithm. Can only deal with nominal attributes. No missing values "
				+ "allowed. Empty leaves may result in unclassified instances. For more " + "information see: \n\n"
				+ getTechnicalInformation().toString();
	}

	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed
	 * information about the technical background of this class, e.g., paper
	 * reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result;

		result = new TechnicalInformation(Type.ARTICLE);
		result.setValue(Field.AUTHOR, "R. Quinlan");
		result.setValue(Field.YEAR, "1986");
		result.setValue(Field.TITLE, "Induction of decision trees");
		result.setValue(Field.JOURNAL, "Machine Learning");
		result.setValue(Field.VOLUME, "1");
		result.setValue(Field.NUMBER, "1");
		result.setValue(Field.PAGES, "81-106");

		return result;
	}

	/**
	 * Returns default capabilities of the classifier.
	 *
	 * @return the capabilities of this classifier
	 */
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);

		// class
		result.enable(Capability.NOMINAL_CLASS);
		result.enable(Capability.MISSING_CLASS_VALUES);

		// instances
		result.setMinimumNumberInstances(0);

		return result;
	}

	/**
	 * Builds Id3 decision tree classifier.
	 *
	 * @param data the training data
	 * @exception Exception if classifier can't be built successfully
	 */
	public void buildClassifier(Instances data) throws Exception {

		// can classifier handle the data?
		getCapabilities().testWithFail(data);

		// remove instances with missing class
		data = new Instances(data);
		data.deleteWithMissingClass();

		MyOneR one_r = new MyOneR();
		one_r.buildClassifier(data);
		weights = one_r.getWeights();

		makeTree(data, weights);
	}

	/**
	 * Method for building an Id3 tree.
	 *
	 * @param data the training data
	 * @exception Exception if decision tree can't be built successfully
	 */
	private void makeTree(Instances data, Map<String, Double> weights) throws Exception {

		// Check if no instances have reached this node.
		if (data.numInstances() == 0) {
			m_Attribute = null;
			m_ClassValue = Utils.missingValue();
			m_Distribution = new double[data.numClasses()];
			return;
		}
		// Compute attribute with maximum information gain.
		// ���Դ��ÿ�ֻ����������ܴ������ؼ�
		double[] infoGains = new double[data.numAttributes()];

		// ÿ�����Ե�splitInfo
		double[] splitInfos = new double[data.numAttributes()];

		// ���ֻ������Ե�ö����
		Enumeration attEnum = data.enumerateAttributes();
		// ����ÿһ������
		while (attEnum.hasMoreElements()) {
			Attribute att = (Attribute) attEnum.nextElement();
			// ����ÿһ���������Ե��ؼ�ֵ
			infoGains[att.index()] = computeInfoGain(data, att, weights);
			splitInfos[att.index()] = computeSplitInfo(data, att, weights);
		}

		// �����ƽ��ֵ
		double avrEntr = 0;
		int count = 0;
		for (double infoGain : infoGains) {
			avrEntr += infoGain;
			count++;

		}
		avrEntr /= count;
		// ���Ҵ���ƽ��ֵ�����棬�������������ʲ������candidates��������
		boolean haveCandidates = false;
		double[] candidates = new double[infoGains.length];
		for (int i = 0; i < infoGains.length; i++) {
			if (infoGains[i] > avrEntr) {
				candidates[i] = infoGains[i] / splitInfos[i];
				haveCandidates = true;
			}
		}
		// �Ľ�2
		// �ж�������Ϣ�ػ�����Ϣ����
		if (haveCandidates) {
			m_Attribute = data.attribute(Utils.maxIndex(candidates));
		} else {
			m_Attribute = data.attribute(Utils.maxIndex(infoGains));
		}
		 //m_Attribute = data.attribute(Utils.maxIndex(infoGains));

		if (Utils.eq(infoGains[m_Attribute.index()], 0)) {
			m_Attribute = null;
			m_Distribution = new double[data.numClasses()];
			Enumeration instEnum = data.enumerateInstances();
			while (instEnum.hasMoreElements()) {
				Instance inst = (Instance) instEnum.nextElement();
				m_Distribution[(int) inst.classValue()]++;
			}
			Utils.normalize(m_Distribution);
			m_ClassValue = Utils.maxIndex(m_Distribution);
			m_ClassAttribute = data.classAttribute();
		} else {
			Instances[] splitData = splitData(data, m_Attribute);
			m_Successors = new MyId3[m_Attribute.numValues()];
			for (int j = 0; j < m_Attribute.numValues(); j++) {
				m_Successors[j] = new MyId3();
				m_Successors[j].makeTree(splitData[j], weights);
			}
		}
	}

	/**
	 * Computes information gain for an attribute.
	 *
	 * @param data the data for which info gain is to be computed
	 * @param att  the attribute
	 * @return the information gain for the given attribute and data
	 * @throws Exception if computation fails
	 */
	private double computeInfoGain(Instances data, Attribute att, Map<String, Double> weights) throws Exception {
		// ���㸸���Ĳ����ȶ����������Ǽ�������
		double infoGain = computeEntropy(data);
		// ��ǰ�������Ե�ÿһ��ȡֵ����
		Instances[] splitData = splitData(data, att);
		// ���㵱ǰ�������ԣ�Ҳ����������������Ի��ֺ󣬱�ɵļ����������ء������ø��ڵ���ؼ���������������
		for (int j = 0; j < att.numValues(); j++) {
			if (splitData[j].numInstances() > 0) {
				infoGain -= ((double) splitData[j].numInstances() / (double) data.numInstances())
						* computeEntropy(splitData[j]);
			}
		}
		// �Ľ�1
		infoGain *= weights.get(att.name());
		return infoGain;
	}

	/**
	 * Classifies a given test instance using the decision tree.
	 *
	 * @param instance the instance to be classified
	 * @return the classification
	 * @throws NoSupportForMissingValuesException if instance has missing values
	 */
	public double classifyInstance(Instance instance) throws NoSupportForMissingValuesException {

		if (instance.hasMissingValue()) {
			throw new NoSupportForMissingValuesException("Id3: no missing values, " + "please.");
		}
		if (m_Attribute == null) {
			return m_ClassValue;
		} else {
			return m_Successors[(int) instance.value(m_Attribute)].classifyInstance(instance);
		}
	}

	/**
	 * Computes class distribution for instance using decision tree.
	 *
	 * @param instance the instance for which distribution is to be computed
	 * @return the class distribution for the given instance
	 * @throws NoSupportForMissingValuesException if instance has missing values
	 */
	public double[] distributionForInstance(Instance instance) throws NoSupportForMissingValuesException {

		if (instance.hasMissingValue()) {
			throw new NoSupportForMissingValuesException("Id3: no missing values, " + "please.");
		}
		if (m_Attribute == null) {
			return m_Distribution;
		} else {
			return m_Successors[(int) instance.value(m_Attribute)].distributionForInstance(instance);
		}
	}

	/**
	 * Prints the decision tree using the private toString method from below.
	 *
	 * @return a textual description of the classifier
	 */
	public String toString() {

		if ((m_Distribution == null) && (m_Successors == null)) {
			return "Id3: No model built yet.";
		}
		return "Id3\n\n" + toString(0);
	}

	private double computeSplitInfo(Instances data, Attribute att, Map<String, Double> weights) throws Exception {
		// ���㸸���Ĳ����ȶ����������Ǽ�������
		// ��ǰ�������Ե�ÿһ��ȡֵ����
		Instances[] splitData = splitData(data, att);

		// �Ľ�2
		double splitInfo = 0;
		for (int i = 0; i < splitData.length; i++) {
			splitInfo -= splitData[i].size() * Utils.log2(splitData[i].size());
		}
		splitInfo /= (double) data.numInstances();
		splitInfo += Utils.log2(data.numInstances());

		return splitInfo;
	}

	/**
	 * Computes the entropy of a dataset.
	 * 
	 * @param data the data for which entropy is to be computed
	 * @return the entropy of the data's class distribution
	 * @throws Exception if computation fails
	 */
	private double computeEntropy(Instances data) throws Exception {

		// ��ȡ�����ǩ�����������������ֻ�С��á�������������
		double[] classCounts = new double[data.numClasses()];
		// ��������ö����
		Enumeration instEnum = data.enumerateInstances();
		while (instEnum.hasMoreElements()) {
			// ����ÿһ������
			Instance inst = (Instance) instEnum.nextElement();
			// ͳ���������ǩ�����Ϻû�������Ŀ
			classCounts[(int) inst.classValue()]++;
		}
		// ������
		double entropy = 0;
		for (int j = 0; j < data.numClasses(); j++) {
			if (classCounts[j] > 0) {
				entropy -= classCounts[j] * Utils.log2(classCounts[j]);
			}
		}
		entropy /= (double) data.numInstances();
		return entropy + Utils.log2(data.numInstances());
	}

	/**
	 * Splits a dataset according to the values of a nominal attribute.
	 *
	 * @param data the data which is to be split
	 * @param att  the attribute to be used for splitting
	 * @return the sets of instances produced by the split
	 */
	private Instances[] splitData(Instances data, Attribute att) {
		// �������Ե�ֵ����
		Instances[] splitData = new Instances[att.numValues()];
		// ��ʼ��һ��
		for (int j = 0; j < att.numValues(); j++) {
			splitData[j] = new Instances(data, data.numInstances());
		}
		// ��������������ͳ�Ƹû�������ÿһ��ȡֵ�ĸ���
		Enumeration instEnum = data.enumerateInstances();
		while (instEnum.hasMoreElements()) {
			Instance inst = (Instance) instEnum.nextElement();
			splitData[(int) inst.value(att)].add(inst);
		}
		// ��������list�������õ��ڴ��ֵ�ĸ���
		for (int i = 0; i < splitData.length; i++) {
			splitData[i].compactify();
		}
		return splitData;
	}

	/**
	 * Outputs a tree at a certain level.
	 *
	 * @param level the level at which the tree is to be printed
	 * @return the tree as string at the given level
	 */
	private String toString(int level) {

		StringBuffer text = new StringBuffer();

		if (m_Attribute == null) {
			if (Utils.isMissingValue(m_ClassValue)) {
				text.append(": null");
			} else {
				text.append(": " + m_ClassAttribute.value((int) m_ClassValue));
			}
		} else {
			for (int j = 0; j < m_Attribute.numValues(); j++) {
				text.append("\n");
				for (int i = 0; i < level; i++) {
					text.append("|  ");
				}
				text.append(m_Attribute.name() + " = " + m_Attribute.value(j));
				text.append(m_Successors[j].toString(level + 1));
			}
		}
		return text.toString();
	}

	/**
	 * Adds this tree recursively to the buffer.
	 * 
	 * @param id     the unqiue id for the method
	 * @param buffer the buffer to add the source code to
	 * @return the last ID being used
	 * @throws Exception if something goes wrong
	 */
	protected int toSource(int id, StringBuffer buffer) throws Exception {
		int result;
		int i;
		int newID;
		StringBuffer[] subBuffers;

		buffer.append("\n");
		buffer.append("  protected static double node" + id + "(Object[] i) {\n");

		// leaf?
		if (m_Attribute == null) {
			result = id;
			if (Double.isNaN(m_ClassValue)) {
				buffer.append("    return Double.NaN;");
			} else {
				buffer.append("    return " + m_ClassValue + ";");
			}
			if (m_ClassAttribute != null) {
				buffer.append(" // " + m_ClassAttribute.value((int) m_ClassValue));
			}
			buffer.append("\n");
			buffer.append("  }\n");
		} else {
			buffer.append("    checkMissing(i, " + m_Attribute.index() + ");\n\n");
			buffer.append("    // " + m_Attribute.name() + "\n");

			// subtree calls
			subBuffers = new StringBuffer[m_Attribute.numValues()];
			newID = id;
			for (i = 0; i < m_Attribute.numValues(); i++) {
				newID++;

				buffer.append("    ");
				if (i > 0) {
					buffer.append("else ");
				}
				buffer.append(
						"if (((String) i[" + m_Attribute.index() + "]).equals(\"" + m_Attribute.value(i) + "\"))\n");
				buffer.append("      return node" + newID + "(i);\n");

				subBuffers[i] = new StringBuffer();
				newID = m_Successors[i].toSource(newID, subBuffers[i]);
			}
			buffer.append("    else\n");
			buffer.append("      throw new IllegalArgumentException(\"Value '\" + i[" + m_Attribute.index()
					+ "] + \"' is not allowed!\");\n");
			buffer.append("  }\n");

			// output subtree code
			for (i = 0; i < m_Attribute.numValues(); i++) {
				buffer.append(subBuffers[i].toString());
			}
			subBuffers = null;

			result = newID;
		}

		return result;
	}

	/**
	 * Returns a string that describes the classifier as source. The classifier will
	 * be contained in a class with the given name (there may be auxiliary classes),
	 * and will contain a method with the signature:
	 * 
	 * <pre>
	 * <code>
	 * public static double classify(Object[] i);
	 * </code>
	 * </pre>
	 * 
	 * where the array <code>i</code> contains elements that are either Double,
	 * String, with missing values represented as null. The generated code is public
	 * domain and comes with no warranty. <br/>
	 * Note: works only if class attribute is the last attribute in the dataset.
	 *
	 * @param className the name that should be given to the source class.
	 * @return the object source described by a string
	 * @throws Exception if the source can't be computed
	 */
	public String toSource(String className) throws Exception {
		StringBuffer result;
		int id;

		result = new StringBuffer();

		result.append("class " + className + " {\n");
		result.append("  private static void checkMissing(Object[] i, int index) {\n");
		result.append("    if (i[index] == null)\n");
		result.append("      throw new IllegalArgumentException(\"Null values " + "are not allowed!\");\n");
		result.append("  }\n\n");
		result.append("  public static double classify(Object[] i) {\n");
		id = 0;
		result.append("    return node" + id + "(i);\n");
		result.append("  }\n");
		toSource(id, result);
		result.append("}\n");

		return result.toString();
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 8109 $");
	}

	/**
	 * Main method.
	 *
	 * @param args the options for the classifier
	 */
	public static void main(String[] args) {
		runClassifier(new MyId3(), args);
	}

	public int graphType() {
		return Drawable.TREE;
	}

	/**
	 * Returns a string that describes the ID3 tree. The output is in dotty format.
	 *
	 * @return the graph described by a string
	 * @throws Exception if the graph can't be computed
	 */
	public String graph() throws Exception {
		StringBuffer text = new StringBuffer();

		text.append("digraph ID3Tree {\n");
		text.append(graph(0));

		return text.toString() + "}\n";
	}

	/**
	 * Returns a string that describes the ID3 tree. The output is in dotty format.
	 *
	 * @param level the level at which the tree is to be printed
	 * @return the graph described by a string
	 * @throws Exception if the graph can't be computed
	 */
	public String graph(int level) throws Exception {
		StringBuffer text = new StringBuffer();
		if (m_Attribute == null) {
			if (Utils.isMissingValue(m_ClassValue)) {
				text.append("N" + m_id + " [label=\"null\" " + "shape=box style=filled ");
			} else {
				text.append("N" + m_id + " [label=\"" + m_ClassAttribute.value((int) m_ClassValue) + "\" "
						+ "shape=box style=filled ");
			}
			text.append("]\n");
		} else {
			text.append("N" + m_id + " [label=\"" + m_Attribute.name() + "\" ");
			text.append("]\n");
			for (int j = 0; j < m_Attribute.numValues(); j++) {
				text.append("N" + m_id + "->" + "N" + m_Successors[j].m_id + " [label=\"= " + m_Attribute.value(j)
						+ "\"]\n");
				text.append(m_Successors[j].graph(level + 1));
			}
		}
		return text.toString();
	}
}
