/**
 * A class to calculate statistics from QGSTEC2010 evaluation submissions.
 */
package com.googlecode.mrsqg.evaluation;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * @author Xuchen Yao
 *
 */
public class Rater {

	private TestXmlParser parser;

	protected ArrayList<EvalInstance> instanceList;
	public static final String RELEVANCE="relevance";
	public static final String QUESTIONTYPE="questionType";
	public static final String CORRECTNESS="correctness";
	public static final String AMBIGUITY="ambiguity";
	public static final String VARIETY="variety";

	public static HashMap<String, Integer> worstScoreMap;
	public static HashSet<String> criterionSet;

	private EvalInstance currentInstance;
	private String currentSubmitter;
	private Submission currentSubmission = null;
	private String currentQuestionType;
	private Question currentQuestion = null;

	public Rater () {
		instanceList = new ArrayList<EvalInstance>();
		worstScoreMap = new LinkedHashMap<String, Integer>();
		worstScoreMap.put(RELEVANCE, 4);
		worstScoreMap.put(QUESTIONTYPE, 2);
		worstScoreMap.put(CORRECTNESS, 4);
		worstScoreMap.put(AMBIGUITY, 3);
		worstScoreMap.put(VARIETY, 3);
		criterionSet = new LinkedHashSet<String>();
		criterionSet.add(RELEVANCE);
		criterionSet.add(QUESTIONTYPE);
		criterionSet.add(CORRECTNESS);
		criterionSet.add(AMBIGUITY);
		criterionSet.add(VARIETY);
		parser = new TestXmlParser();
	}

	public Rater (File file) {
		this();
		this.parser.parse(file);
	}

	/**
	 * This method parses a QGSTEC2010 Evaluation document in XML.
	 *
	 * @param file an XML file
	 */
	public void parse(File file) {
		this.parser.parse(file);
	}

	/**
	 * Calculate the average scores for all ratings
	 */
	public void average () {
		// <submitter, <RatingCriterion, Score>>
		LinkedHashMap<String, HashMap<String, Float>> scoreMap = new LinkedHashMap<String, HashMap<String, Float>>();
		// with penalty for missing questions
		LinkedHashMap<String, HashMap<String, Float>> scoreMapPenalty = new LinkedHashMap<String, HashMap<String, Float>>();
		// <submitter, questionCount>
		LinkedHashMap<String, Integer> questionCountMap = new LinkedHashMap<String, Integer>();
		// with penalty for missing questions
		LinkedHashMap<String, Integer> countMapPenalty = new LinkedHashMap<String, Integer>();
		// <submitter, sententCount>
		LinkedHashMap<String, Integer> sentCountMap = new LinkedHashMap<String, Integer>();
		// <submitter, sentenceLengthList>
		LinkedHashMap<String, ArrayList<Integer>> sentLenListMap = new LinkedHashMap<String, ArrayList<Integer>>();
		// <submitter, sentenceMeanLength>
		LinkedHashMap<String, Double> sentMeanMap = new LinkedHashMap<String, Double>();
		// <submitter, sentenceStdLength>
		LinkedHashMap<String, Double> sentStdMap = new LinkedHashMap<String, Double>();
		// <submitter, questionLengthList>
		LinkedHashMap<String, ArrayList<Integer>> questionLenListMap = new LinkedHashMap<String, ArrayList<Integer>>();
		// <submitter, questionMeanLength>
		LinkedHashMap<String, Double> questionMeanMap = new LinkedHashMap<String, Double>();
		// <submitter, questionStdLength>
		LinkedHashMap<String, Double> questionStdMap = new LinkedHashMap<String, Double>();
		float score, scorePenalty;
		int count = 0, countPenalty = 0;
		for (EvalInstance e:this.instanceList) {
			int sentLen = e.textLen;
			for (String submitter:e.submissionMap.keySet()) {

				// initialization
				if (scoreMap.get(submitter)==null) {
					HashMap<String, Float> mapScore = new HashMap<String, Float>();
					HashMap<String, Float> mapScorePenalty = new HashMap<String, Float>();
					for (String ratingCriterion:criterionSet) {
						mapScore.put(ratingCriterion, 0f);
						mapScorePenalty.put(ratingCriterion, 0f);
					}
					scoreMap.put(submitter, mapScore);
					questionCountMap.put(submitter, 0);
					scoreMapPenalty.put(submitter, mapScorePenalty);
					countMapPenalty.put(submitter, 0);

					sentCountMap.put(submitter, 0);
					sentLenListMap.put(submitter, new ArrayList<Integer>());
					sentMeanMap.put(submitter, 0.0);
					sentStdMap.put(submitter, 0.0);
					questionLenListMap.put(submitter, new ArrayList<Integer>());
					questionMeanMap.put(submitter, 0.0);
					questionStdMap.put(submitter, 0.0);
				}
				Submission submission = e.submissionMap.get(submitter);
				HashMap<String, ArrayList<Question>> typeQuesMap = submission.typeQuesMap;
				if (typeQuesMap.size() != 0) {
					sentLenListMap.get(submitter).add(sentLen);
					sentCountMap.put(submitter, sentCountMap.get(submitter)+1);
				}
				for (String type:e.questionTypeSet) {
					ArrayList<Question> questions = typeQuesMap.get(type);
					int penaltyTimes=0;
					if (questions==null || questions.size()==0) {
						penaltyTimes = 2;
					} else if (questions.size()==1 || questions.size()==2) {
						penaltyTimes = 2 - questions.size();

						// <submitter, <RatingCriterion, Score>>
						for (Question q:questions) {
							for (String criterion:criterionSet) {
								score = scoreMap.get(submitter).get(criterion);
								score += q.getScoreByCriterion(criterion);
								scoreMap.get(submitter).put(criterion, score);

								scorePenalty = scoreMapPenalty.get(submitter).get(criterion);
								scorePenalty += q.getScoreByCriterion(criterion);
								scoreMapPenalty.get(submitter).put(criterion, scorePenalty);
							}
							count = questionCountMap.get(submitter);
							questionCountMap.put(submitter, count+1);

							questionLenListMap.get(submitter).add(q.questionLen);

							countPenalty = countMapPenalty.get(submitter);
							countMapPenalty.put(submitter, countPenalty+1);
						}
					} else {
						System.err.println("questions size > 2, debug your code.");
						System.exit(-1);
					}

					// penalty

					if (penaltyTimes != 0) {
						for (String criterion:this.worstScoreMap.keySet()) {
							scorePenalty = scoreMapPenalty.get(submitter).get(criterion);
							// 0, 1, or 2
							scorePenalty += this.worstScoreMap.get(criterion)*penaltyTimes;
							scoreMapPenalty.get(submitter).put(criterion, scorePenalty);
						}

						countPenalty = countMapPenalty.get(submitter);
						countPenalty += penaltyTimes;
						countMapPenalty.put(submitter, countPenalty);
					}

				}

			}

		}

		// average length of sentences and questions
		int sentCount, sentLen, quesCount, quesLen;
		for (String submitter:scoreMap.keySet()) {
			sentCount = sentCountMap.get(submitter);
			if (sentCount != sentLenListMap.get(submitter).size()) {
				System.err.println("Error in sentence count: "+sentCount+"!="+sentLenListMap.get(submitter).size());
				return;
			}
			sentLen = Rater.sumArrayList(sentLenListMap.get(submitter));
			sentMeanMap.put(submitter, sentLen*1.0/sentCountMap.get(submitter));

			quesCount = questionCountMap.get(submitter);
			if (quesCount != questionLenListMap.get(submitter).size()) {
				System.err.println("Error in question count: "+quesCount+"!="+questionLenListMap.get(submitter).size());
				return;
			}
			quesLen = Rater.sumArrayList(questionLenListMap.get(submitter));
			questionMeanMap.put(submitter, quesLen*1.0/questionCountMap.get(submitter));
		}

		// std of sentences and questions
		double std = 0;
		double mean;
		for (String submitter:scoreMap.keySet()) {
			mean = sentMeanMap.get(submitter);
			for (Integer len:sentLenListMap.get(submitter)) {
				std += (len-mean)*(len-mean);
			}
			std /= sentCountMap.get(submitter);
			std = Math.sqrt(std);
			sentStdMap.put(submitter, std);

			std = 0;
			mean = questionMeanMap.get(submitter);
			for (Integer len:questionLenListMap.get(submitter)) {
				std += (len-mean)*(len-mean);
			}
			std /= questionCountMap.get(submitter);
			std = Math.sqrt(std);
			questionStdMap.put(submitter, std);
		}

		System.out.println("\nAverages:");
		for (String submitter:scoreMap.keySet()) {
			//<submitter, <RatingCriterion, Score>>
			System.out.println("\nSubmitter "+submitter+":");
			count = questionCountMap.get(submitter);
			for (String criterion:criterionSet) {
				score = scoreMap.get(submitter).get(criterion);
				score = score/count;
				System.out.print(criterion+String.format(": %.2f\t",score));
			}
			System.out.println("\nSentence Count: "+sentCountMap.get(submitter)+
					" Mean: "+String.format("%.2f",sentMeanMap.get(submitter))+
					" Std: "+String.format("%.2f",sentStdMap.get(submitter)));
			System.out.println("Question Count: "+questionCountMap.get(submitter)+
					" Mean: "+String.format("%.2f",questionMeanMap.get(submitter))+
					" Std: "+String.format("%.2f",questionStdMap.get(submitter)));
		}


		System.out.println("\nAverages with Penalties:");
		for (String submitter:scoreMap.keySet()) {
			//<submitter, <RatingCriterion, Score>>
			System.out.println("\nSubmitter "+submitter+":");
			countPenalty = countMapPenalty.get(submitter);
			for (String criterion:criterionSet) {
				scorePenalty = scoreMapPenalty.get(submitter).get(criterion);
				scorePenalty = scorePenalty/countPenalty;
				System.out.print(criterion+String.format(": %.2f\t",scorePenalty));
			}
			System.out.println("\tTotal Count: "+countPenalty);
		}
		/*

Averages:

Submitter a:
relevance: 1.61	questionType: 1.13	correctness: 2.06	ambiguity: 1.52	variety: 1.78
Sentence Count: 89 Mean: 19.27 Std: 6.94
Question Count: 354 Mean: 12.36 Std: 7.40

Submitter b:
relevance: 1.17	questionType: 1.06	correctness: 1.75	ambiguity: 1.30	variety: 2.08
Sentence Count: 76 Mean: 19.36 Std: 7.21
Question Count: 165 Mean: 13.75 Std: 7.22

Submitter c:
relevance: 1.68	questionType: 1.19	correctness: 2.44	ambiguity: 1.76	variety: 1.86
Sentence Count: 85 Mean: 19.61 Std: 6.91
Question Count: 209 Mean: 13.32 Std: 7.34

Submitter d:
relevance: 1.74	questionType: 1.05	correctness: 2.64	ambiguity: 1.96	variety: 1.76
Sentence Count: 50 Mean: 20.18 Std: 5.75
Question Count: 168 Mean: 8.26 Std: 3.85

Averages with Penalties:

Submitter a:
relevance: 1.65	questionType: 1.15	correctness: 2.09	ambiguity: 1.54	variety: 1.80		Total Count: 360

Submitter b:
relevance: 2.70	questionType: 1.57	correctness: 2.97	ambiguity: 2.22	variety: 2.58		Total Count: 360

Submitter c:
relevance: 2.65	questionType: 1.53	correctness: 3.10	ambiguity: 2.28	variety: 2.34		Total Count: 360

Submitter d:
relevance: 2.95	questionType: 1.56	correctness: 3.36	ambiguity: 2.52	variety: 2.42		Total Count: 360
		 */
	}

	public static int sumArrayList(ArrayList<Integer> list) {
		int s = 0;
		for (Integer l:list)
			s+=l;
		return s;
	}

	/**
	 * Calculate the mean and std of sentences in the test set
	 */
	public void testset() {
		// <Source, entryCount>
		HashMap<String, Integer> SourceCountMap = new HashMap<String, Integer>();
		// <Source, textLength>
		HashMap<String, Integer> SourceLenMap = new HashMap<String, Integer>();
		// <Source, Questions>
		HashMap<String, Integer> SourceQuestionMap = new HashMap<String, Integer>();
		// <Source, mean>
		HashMap<String, Float> SourceMeanMap = new HashMap<String, Float>();
		// <Source, std>
		HashMap<String, Float> SourceStdMap = new HashMap<String, Float>();
		String source;
		int len;
		float sq;
		int countAll=0, lenAll=0, questionAll=0;
		float meanAll=0.0f, stdAll=0.0f;
		for (EvalInstance e:this.instanceList) {
			countAll++;
			source = e.idSource;
			if (!SourceCountMap.containsKey(source)) {
				SourceCountMap.put(source, 0);
				SourceLenMap.put(source, 0);
				SourceQuestionMap.put(source, 0);
			}
			SourceCountMap.put(source, SourceCountMap.get(source)+1);
			len = SourceLenMap.get(source);
			lenAll += e.textLen;
			len += e.textLen;
			SourceLenMap.put(source, len);

			len = SourceQuestionMap.get(source);
			// 2 questions for each type
			len += e.questionTypeSet.size()*2;
			questionAll += e.questionTypeSet.size()*2;
			SourceQuestionMap.put(source, len);
		}
		System.out.println("Test Set");
		// average sentence length
		for (String s:SourceLenMap.keySet()) {
			SourceMeanMap.put(s, (float)(SourceLenMap.get(s)*1.0/SourceCountMap.get(s)));
		}
		meanAll = lenAll*1.0f/countAll;
		// standard deviation
		for (EvalInstance e:this.instanceList) {
			source = e.idSource;
			if (!SourceStdMap.containsKey(source)) {
				SourceStdMap.put(source, 0.0f);
			}
			sq = (e.textLen-SourceMeanMap.get(source))*(e.textLen-SourceMeanMap.get(source));
			sq += SourceStdMap.get(source);
			SourceStdMap.put(source, sq);
			stdAll += (e.textLen-meanAll)*(e.textLen-meanAll);
		}
		stdAll /= countAll;
		stdAll = (float)Math.sqrt(stdAll);
		for (String s:SourceCountMap.keySet()) {
			sq = SourceStdMap.get(s);
			sq /= SourceCountMap.get(s);
			sq = (float)Math.sqrt(sq);
			SourceStdMap.put(s, sq);
		}

		// print
		for (String s:SourceLenMap.keySet()) {
			System.out.println(s+": "+SourceCountMap.get(s)+" entries, average sent length: "+
					String.format("%.2f",SourceMeanMap.get(s))+ " standard deviation: "+
					String.format("%.2f",SourceStdMap.get(s))+" questions: "+SourceQuestionMap.get(s));
		}
		System.out.println("All: "+countAll+" entries, average sent length: "+
				String.format("%.2f",meanAll)+ " standard deviation: "+
				String.format("%.2f",stdAll)+" questions: "+questionAll);
		/*
Test Set
Yahoo! Answers: 35 entries, average sent length: 15.97 standard deviation: 7.16 questions: 120
Wikipedia: 27 entries, average sent length: 22.11 standard deviation: 6.45 questions: 120
OpenLearn: 28 entries, average sent length: 20.43 standard deviation: 5.26 questions: 120
All: 90 entries, average sent length: 19.20 standard deviation: 6.93 questions: 360
		 */
	}


	/**
	 * Only calculate A based on B, since B is the best system but with a low recall.
	 */
	public void onlyAB() {
		// <submitter, <RatingCriterion, Score>>
		LinkedHashMap<String, HashMap<String, Float>> scoreMap = new LinkedHashMap<String, HashMap<String, Float>>();
		// with penalty for missing questions
		LinkedHashMap<String, HashMap<String, Float>> scoreMapPenalty = new LinkedHashMap<String, HashMap<String, Float>>();
		// <submitter, questionCount>
		LinkedHashMap<String, Integer> questionCountMap = new LinkedHashMap<String, Integer>();
		// with penalty for missing questions
		LinkedHashMap<String, Integer> countMapPenalty = new LinkedHashMap<String, Integer>();
		// <submitter, sententCount>
		LinkedHashMap<String, Integer> sentCountMap = new LinkedHashMap<String, Integer>();

		String[] submitterAB = {"b", "a"};

		float score, scorePenalty;
		int count = 0, countPenalty = 0;
		for (EvalInstance e:this.instanceList) {
			int sentLen = e.textLen;
			for (String submitter:submitterAB) {

				// initialization
				if (scoreMap.get(submitter)==null) {
					HashMap<String, Float> mapScore = new HashMap<String, Float>();
					HashMap<String, Float> mapScorePenalty = new HashMap<String, Float>();
					for (String ratingCriterion:criterionSet) {
						mapScore.put(ratingCriterion, 0f);
						mapScorePenalty.put(ratingCriterion, 0f);
					}
					scoreMap.put(submitter, mapScore);
					questionCountMap.put(submitter, 0);
					scoreMapPenalty.put(submitter, mapScorePenalty);
					countMapPenalty.put(submitter, 0);

					sentCountMap.put(submitter, 0);
				}
				Submission submission = e.submissionMap.get(submitter);
				Submission submissionB = e.submissionMap.get("b");
				HashMap<String, ArrayList<Question>> typeQuesMap = submission.typeQuesMap;
				HashMap<String, ArrayList<Question>> typeQuesMapB = submissionB.typeQuesMap;
				boolean addSentCount = false;
				for (String type:e.questionTypeSet) {
					ArrayList<Question> questions = typeQuesMap.get(type);
					ArrayList<Question> questionsB = typeQuesMapB.get(type);
					int penaltyTimes=0;
					if (questionsB==null || questionsB.size()==0) {
						// b got nothing, we don't count a either
						continue;
					}
					addSentCount = true;
					if (questions==null || questions.size()==0) {
						penaltyTimes = 2;
					} else if (questions.size()==1 || questions.size()==2) {
						penaltyTimes = 2 - questions.size();

						// <submitter, <RatingCriterion, Score>>
						for (Question q:questions) {
							for (String criterion:criterionSet) {
								score = scoreMap.get(submitter).get(criterion);
								score += q.getScoreByCriterion(criterion);
								scoreMap.get(submitter).put(criterion, score);

								scorePenalty = scoreMapPenalty.get(submitter).get(criterion);
								scorePenalty += q.getScoreByCriterion(criterion);
								scoreMapPenalty.get(submitter).put(criterion, scorePenalty);
							}
							count = questionCountMap.get(submitter);
							questionCountMap.put(submitter, count+1);

							countPenalty = countMapPenalty.get(submitter);
							countMapPenalty.put(submitter, countPenalty+1);
						}
					} else {
						System.err.println("questions size > 2, debug your code.");
						System.exit(-1);
					}

					// penalty

					if (penaltyTimes != 0) {
						for (String criterion:this.worstScoreMap.keySet()) {
							scorePenalty = scoreMapPenalty.get(submitter).get(criterion);
							// 0, 1, or 2
							scorePenalty += this.worstScoreMap.get(criterion)*penaltyTimes;
							scoreMapPenalty.get(submitter).put(criterion, scorePenalty);
						}

						countPenalty = countMapPenalty.get(submitter);
						countPenalty += penaltyTimes;
						countMapPenalty.put(submitter, countPenalty);
					}

				}
				if (addSentCount && typeQuesMap.size() != 0) {
					sentCountMap.put(submitter, sentCountMap.get(submitter)+1);
				}
			}

		}


		System.out.println("\nAverages:");
		for (String submitter:scoreMap.keySet()) {
			//<submitter, <RatingCriterion, Score>>
			System.out.println("\nSubmitter "+submitter+":");
			count = questionCountMap.get(submitter);
			for (String criterion:criterionSet) {
				score = scoreMap.get(submitter).get(criterion);
				score = score/count;
				System.out.print(criterion+String.format(": %.2f\t",score));
			}
			System.out.println("\nSentence Count: "+sentCountMap.get(submitter));
			System.out.println("Question Count: "+questionCountMap.get(submitter));
		}


		System.out.println("\n\nAverages with Penalties:");
		for (String submitter:scoreMap.keySet()) {
			//<submitter, <RatingCriterion, Score>>
			System.out.println("\nSubmitter "+submitter+":");
			countPenalty = countMapPenalty.get(submitter);
			for (String criterion:criterionSet) {
				scorePenalty = scoreMapPenalty.get(submitter).get(criterion);
				scorePenalty = scorePenalty/countPenalty;
				System.out.print(criterion+String.format(": %.2f\t",scorePenalty));
			}
			System.out.println("\tTotal Count: "+countPenalty);
		}
		/*
Averages:

Submitter b:
relevance: 1.17	questionType: 1.06	correctness: 1.75	ambiguity: 1.30	variety: 2.08
Sentence Count: 76
Question Count: 165

Submitter a:
relevance: 1.57	questionType: 1.14	correctness: 1.99	ambiguity: 1.49	variety: 1.67
Sentence Count: 76
Question Count: 246


Averages with Penalties:

Submitter b:
relevance: 2.13	questionType: 1.38	correctness: 2.51	ambiguity: 1.88	variety: 2.40		Total Count: 250

Submitter a:
relevance: 1.61	questionType: 1.15	correctness: 2.02	ambiguity: 1.52	variety: 1.69		Total Count: 250
		 */
	}

	public void onlyABstrict() {
		// <submitter, <RatingCriterion, Score>>
		LinkedHashMap<String, HashMap<String, Float>> scoreMap = new LinkedHashMap<String, HashMap<String, Float>>();
		// <submitter, questionCount>
		LinkedHashMap<String, Integer> questionCountMap = new LinkedHashMap<String, Integer>();
		// <submitter, sententCount>
		LinkedHashMap<String, Integer> sentCountMap = new LinkedHashMap<String, Integer>();

		String[] submitterAB = {"b", "a"};

		float score;
		int count = 0, countPenalty = 0;
		for (EvalInstance e:this.instanceList) {
			for (String submitter:submitterAB) {

				// initialization
				if (scoreMap.get(submitter)==null) {
					HashMap<String, Float> mapScore = new HashMap<String, Float>();
					HashMap<String, Float> mapScorePenalty = new HashMap<String, Float>();
					for (String ratingCriterion:criterionSet) {
						mapScore.put(ratingCriterion, 0f);
						mapScorePenalty.put(ratingCriterion, 0f);
					}
					scoreMap.put(submitter, mapScore);
					questionCountMap.put(submitter, 0);

					sentCountMap.put(submitter, 0);
				}
				Submission submission = e.submissionMap.get(submitter);
				Submission submissionB = e.submissionMap.get("b");
				HashMap<String, ArrayList<Question>> typeQuesMap = submission.typeQuesMap;
				HashMap<String, ArrayList<Question>> typeQuesMapB = submissionB.typeQuesMap;
				boolean addSentCount = false;
				for (String type:e.questionTypeSet) {
					ArrayList<Question> questions = typeQuesMap.get(type);
					ArrayList<Question> questionsB = typeQuesMapB.get(type);
					int penaltyTimes=0;
					if (questionsB==null || questionsB.size()==0) {
						// b got nothing, we don't count a either
						continue;
					}
					addSentCount = true;
					if (questions==null || questions.size()==0) {
						penaltyTimes = 2;
					} else if (questions.size()==1 || questions.size()==2) {
						penaltyTimes = 2 - questions.size();

						if (submitter.equals("a") && questionsB.size()==1) {
							// <submitter, <RatingCriterion, Score>>
							float[] score12 = {0f,0f};
							int lowest;
							for (int i=0; i<questions.size(); i++) {
								Question q = questions.get(i);
								for (String criterion:criterionSet) {
									score12[i] = q.getScoreByCriterion(criterion);
								}
							}
							lowest = score12[0]>score12[1]?1:0;
							Question q = questions.get(lowest);
							for (String criterion:criterionSet) {
								score = scoreMap.get(submitter).get(criterion);
								score += q.getScoreByCriterion(criterion);
								scoreMap.get(submitter).put(criterion, score);
							}

							count = questionCountMap.get(submitter);
							questionCountMap.put(submitter, count+1);
						} else {
							// <submitter, <RatingCriterion, Score>>
							for (Question q:questions) {
								for (String criterion:criterionSet) {
									score = scoreMap.get(submitter).get(criterion);
									score += q.getScoreByCriterion(criterion);
									scoreMap.get(submitter).put(criterion, score);

								}
								count = questionCountMap.get(submitter);
								questionCountMap.put(submitter, count+1);

							}
						}
					} else {
						System.err.println("questions size > 2, debug your code.");
						System.exit(-1);
					}


				}
				if (addSentCount && typeQuesMap.size() != 0) {
					sentCountMap.put(submitter, sentCountMap.get(submitter)+1);
				}
			}

		}


		System.out.println("\nAverages:");
		for (String submitter:scoreMap.keySet()) {
			//<submitter, <RatingCriterion, Score>>
			System.out.println("\nSubmitter "+submitter+":");
			count = questionCountMap.get(submitter);
			for (String criterion:criterionSet) {
				score = scoreMap.get(submitter).get(criterion);
				score = score/count;
				System.out.print(criterion+String.format(": %.2f\t",score));
			}
			System.out.println("\nSentence Count: "+sentCountMap.get(submitter));
			System.out.println("Question Count: "+questionCountMap.get(submitter));
		}
		/*

Averages:

Submitter b:
relevance: 1.17	questionType: 1.06	correctness: 1.75	ambiguity: 1.30	variety: 2.08
Sentence Count: 76
Question Count: 165

Submitter a:
relevance: 1.56	questionType: 1.13	correctness: 1.97	ambiguity: 1.44	variety: 1.68
Sentence Count: 76
Question Count: 162
		 */

	}

	private class TestXmlParser extends DefaultHandler {

		private Stack<String> stack;
		private StringBuilder chars;

		public TestXmlParser () {
			super();
			this.stack = new Stack<String>();
			this.chars = new StringBuilder();
		}

		public void parse(File file) {
			try {
				XMLReader xr = XMLReaderFactory.createXMLReader();
				TestXmlParser handler = new TestXmlParser();
				xr.setContentHandler(handler);
				xr.setErrorHandler(handler);

				FileReader r = new FileReader(file);
				xr.parse(new InputSource(r));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void parseString(String str) {
			try {
				XMLReader xr = XMLReaderFactory.createXMLReader();
				TestXmlParser handler = new TestXmlParser();
				xr.setContentHandler(handler);
				xr.setErrorHandler(handler);

				StringReader r = new StringReader(str);
				xr.parse(new InputSource(r));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void startDocument ()
		{
			//System.out.println("Start document");
		}

		public void endDocument ()
		{
			//System.out.println("End document");
		}

		public void startElement (String uri, String name,
				String qName, Attributes atts)
		{
			String raw, parent;

			if (qName.equals("dataset")) {
				// if stack is not empty, then error
				if (stack.empty() == false) {
					System.err.println("Error, non-empty stack: " +
					"<dataset> shouldn't have parent element");
				}
			} else if (qName.equals("instance")) {
				// <instance id="1">
				raw = atts.getValue("id");

				currentInstance = new EvalInstance();

				currentInstance.idNum = raw;

			} else if (qName.equals("targetQuestionType")) {
				// <targetQuestionType>what</targetQuestionType>
				// do nothing
			} else if (qName.equals("submission")) {
				// <submission id="a">
				currentSubmitter = atts.getValue("id");
				currentSubmission = new Submission();
			} else if (qName.equals("question")) {
				// <question type="how many">
				currentQuestionType = atts.getValue("type");
				currentQuestion = new Question();
				parent = stack.peek();
			} else if (qName.equals("rating")) {
				// <rating rater="ss" relevance="1" questionType="1" correctness="4" ambiguity="1" variety="2" />
				parent = stack.peek();
				if (parent.equals("question")) {
					// the first rating
					currentQuestion.question = chars.toString();
					currentQuestion.setQuestionLen();
					currentQuestion.rater1 = atts.getValue("rater");
					currentQuestion.relevance1 = Integer.parseInt(atts.getValue(RELEVANCE).trim());
					currentQuestion.questionType1 = Integer.parseInt(atts.getValue(QUESTIONTYPE).trim());
					currentQuestion.correctness1 = Integer.parseInt(atts.getValue(CORRECTNESS).trim());
					currentQuestion.ambiguity1 = Integer.parseInt(atts.getValue(AMBIGUITY).trim());
					currentQuestion.variety1 = Integer.parseInt(atts.getValue(VARIETY).trim());
				} else if (parent.equals("rating")) {
					// the second rating
					currentQuestion.rater2 = atts.getValue("rater");
					currentQuestion.relevance2 = Integer.parseInt(atts.getValue(RELEVANCE).trim());
					currentQuestion.questionType2 = Integer.parseInt(atts.getValue(QUESTIONTYPE).trim());
					currentQuestion.correctness2 = Integer.parseInt(atts.getValue(CORRECTNESS).trim());
					currentQuestion.ambiguity2 = Integer.parseInt(atts.getValue(AMBIGUITY).trim());
					currentQuestion.variety2 = Integer.parseInt(atts.getValue(VARIETY).trim());
					currentQuestion.calcAverage();
					currentQuestion.determinBetter();
				} else {
					System.err.println("Stack is wrong: "+parent);
				}
			}
			chars = new StringBuilder();
			stack.push(qName);
		}

		public void endElement (String uri, String name, String qName)
		{
			String raw;
			if (qName.equals("instance")) {
				if (currentInstance != null)
					instanceList.add(currentInstance);
				else
					System.err.println("currentInstance shouldn't be none!");
			} else if (qName.equals("id")) {
				// <id>OpenLearn</id>
				raw = chars.toString();
				currentInstance.idSource = raw;
			} else if (qName.equals("source")) {
				// <source>A103_3</source>
				raw = chars.toString();
				currentInstance.source = raw;
			} else if (qName.equals("text")) {
				// <text>...</text>
				raw = chars.toString();
				currentInstance.text = raw;
				currentInstance.setTextLen();
			} else if (qName.equals("targetQuestionType")) {
				// <targetQuestionType>what</targetQuestionType>
				raw = chars.toString();
				currentInstance.questionTypeSet.add(raw);
			} else if (qName.equals("submission")) {
				// </submission>
				// currentInstance.processEndElement(qName, chars.toString());
				if (currentSubmission != null)
					currentInstance.submissionMap.put(currentSubmitter, currentSubmission);
				currentSubmission = null;
			} else if (qName.equals("question")) {
				if (currentSubmission.typeQuesMap.get(currentQuestionType) == null)
					currentSubmission.typeQuesMap.put(currentQuestionType, new ArrayList<Question>());
				currentSubmission.typeQuesMap.get(currentQuestionType).add(currentQuestion);
				// pop two "rating" out
				stack.pop();
				stack.pop();
			} else if (qName.equals("rating")) {
				// push it back to stack again so we know which "rating" comes first
				stack.push(qName);
			}
			stack.pop();
		}

		public void characters (char ch[], int start, int length)
		{
			chars.append(ch, start, length);
		}

	}

	private class EvalInstance {
		/**  <instance id="1"> */
		protected String idNum;
		/** <id>OpenLearn</id> */
		protected String idSource;
		/** <source>A103_3</source> */
		protected String source;
		/** <text>The view that ... </text>*/
		protected String text;
		/** the length of text in words */
		protected int textLen;
		/**
		 * <question type="how many"></question>
		 */
		protected HashSet<String> questionTypeSet;
		/**
		 * <submission id="a">
		 * a mapping between submitter and its submission
		 */
		protected LinkedHashMap<String, Submission> submissionMap;

		public EvalInstance() {
			questionTypeSet = new HashSet<String>();
			submissionMap = new LinkedHashMap<String, Submission>();
		}

		public void setTextLen() {
			textLen = text.split("\\s+").length;
		}

	}

	private class Submission {
		/**
		 * a mapping between question type and the list of
		 * generated questions
		 */
		protected HashMap<String, ArrayList<Question>> typeQuesMap;

		public Submission() {
			typeQuesMap = new HashMap<String, ArrayList<Question>>();
		}
	}

	private class Question {
		protected String question;
		protected int questionLen;
		protected String rater1;
		protected String rater2;
		protected float relevance1;
		protected float relevance2;
		protected float relevance;
		protected float questionType1;
		protected float questionType2;
		protected float questionType;
		protected float correctness1;
		protected float correctness2;
		protected float correctness;
		protected float ambiguity1;
		protected float ambiguity2;
		protected float ambiguity;
		protected float variety1;
		protected float variety2;
		protected float variety;
		private boolean oneIsBetter;

		public void setQuestionLen() {
			questionLen = question.split("\\s+").length;
		}

		public void calcAverage() {
			relevance = (relevance1+relevance2)/2;
			questionType = (questionType1+questionType2)/2;
			correctness = (correctness1+correctness2)/2;
			ambiguity = (ambiguity1+ambiguity2)/2;
			variety = (variety1+variety2)/2;
		}

		public void determinBetter() {
			if (relevance1+questionType1+correctness1+ambiguity1+variety1<
					relevance2+questionType2+correctness2+ambiguity2+variety2)
				oneIsBetter = true;
			else
				oneIsBetter = false;
		}

		public float getScoreByCriterion (String criterion) {
			if (criterion.equals(RELEVANCE))
				return relevance;
			else if (criterion.equals(QUESTIONTYPE))
				return questionType;
			else if (criterion.equals(CORRECTNESS))
				return correctness;
			else if (criterion.equals(AMBIGUITY))
				return ambiguity;
			else if (criterion.equals(VARIETY))
				return variety;
			else
				return -10000000f;
		}

		public float getBetterScoreByCriterion (String criterion) {
			if (criterion.equals(RELEVANCE))
				return oneIsBetter?relevance1:relevance2;
			else if (criterion.equals(QUESTIONTYPE))
				return oneIsBetter?questionType1:questionType2;
			else if (criterion.equals(CORRECTNESS))
				return oneIsBetter?correctness1:correctness2;
			else if (criterion.equals(AMBIGUITY))
				return oneIsBetter?ambiguity1:ambiguity2;
			else if (criterion.equals(VARIETY))
				return oneIsBetter?variety1:variety2;
			else
				return -10000000f;
		}
	}

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		File file = new File("/home/xcyao/tex/qg/QGSTECresult/EvaluatedSubmissions.xml");
		Rater r = new Rater(file);
		r.average();
		r.testset();
	}

}
