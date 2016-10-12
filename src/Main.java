import java.util.*;
import java.lang.*;
import java.io.*;
import java.math.*;
class Action {
	int start;
	int end;
	boolean able;//是否有效
	double prob;//概率
}

class Graph {
	public ArrayList<Vertex> vertexes;
}

class Vertex {
	public int order;
	public ArrayList<Integer> neighbours;
	public ArrayList<Action> la;//自动学习机
}

public class Main {
	private static final int Loop = 1000;
	private static final double productOfLoopT = 0;
	private static final int N = 200;
	private static final int vt = N;
    private static final double reward = 0.06;//奖励参数
    private static final double penalty = 0.04;//惩罚参数
	
	private Graph graph; 
	private ArrayList<HashSet<Integer>> IS;//所有T轮的最大独立集
	
	public static void main(String[] args) {
		Main main = new Main();

		main.readGraph();
		main.init();
		int t = 1;
		double productOfLoopZero = main.productOfMaximumProbability(0);

		/*尝试对vt节点的行为也进行记录,并奖励与惩罚*/
		Vertex vertexT = new Vertex();
		vertexT.order = N;
		vertexT.neighbours = new ArrayList<Integer>(N);
		vertexT.la = new ArrayList<Action>(N);
		for(int i = 0; i < N; i++) {
			vertexT.neighbours.add(i);
			Action tempA = new Action();
			tempA.start = N;
			tempA.end = i;
			tempA.able = true;
			tempA.prob = 1/(double)N;
			vertexT.la.add(tempA);
		}



		while(t <= Loop) {//先只用一个条件
			if(main.IS.get(t-1).size() >= 10) {
				System.out.println(main.IS.get(t-1).size()+"QQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQ");
			}
			//保存上一次迭代结束后的网络的行为状态，以便后面可能用到
			Graph lastGraph = main.graph;
			Iterator<Vertex> iterator = lastGraph.vertexes.iterator();
			int count = 0;
			while(iterator.hasNext()) {
				Iterator<Action> iterator1 = iterator.next().la.iterator();
				int count1  = 0;
				while(iterator1.hasNext()) {
					if(iterator1.next().able == false) {//此时应当将其恢复成默认的trues
//						System.out.println("count = "+count+", count1 = "+count1);
						lastGraph.vertexes.get(count).la.get(count1).able = true;
					}
					count1++;
				}
				count++;
			}

			//开始迭代
			int[] activable = new int[N];//维持一个可激活数组，默认都是1
			ArrayList<Action> chooseActions = new ArrayList<Action>();//维持一个已选action的集合
			for(int i = 0; i < N; i++) {
				activable[i] = 1;
			}
			
			Random ra = new Random();
			int vi = ra.nextInt(N);

			System.out.println("初始节点为："+vi);

//			System.out.println("初始节点加入到t = "+ t +"独立集");
			if(main.addToIS(vi, t)) {//加入到独立集
				main.IS.get(t-1).add(vi);
			}
			activable[vi] = 0;//置为不可激活状态
//			System.out.println(vi+"置为不可激活状态");
			
			main.disableActionsWithVertex(vi);//禁用
//			System.out.println();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap = main.chooseNextVertexByProbability(vi, activable);//选择下一个

			int vj = (int) hashmap.get("next");
//			System.out.println("得到的vj = "+vj);
			Action tempAction = new Action();
			tempAction = (Action)hashmap.get("action");

			boolean vkIsVt = false;//设置一个flag,判定vj的前驱vk是否为vt
			while(main.judgeActivableNumber(activable)) {
				if(vj != vt) {//当vj不是vt点
//					System.out.print("vj!=vt vj = ");
					activable[vj] = 0;//置为不可激活状态
//					System.out.println(vj+"置为不可激活状态");
					main.disableActionsWithVertex(vj);//禁用
					if(vkIsVt == false) {
//						System.out.println("选择的action为："+tempAction.start+"->"+tempAction.end+" tempAction的able为"+tempAction.able);
						chooseActions.add(tempAction);//将action加入到chooseActions中
					}
//					System.out.println();
					HashMap<String, Object> hashmap1 = new HashMap<String, Object>();
					hashmap1 = main.chooseNextVertexByProbability(vj, activable);
					int vk = (int)hashmap1.get("next");//选择下一个
//					System.out.println("vk："+vk);
					tempAction = (Action) hashmap1.get("action");

					if(vk != vt) {
						activable[vk] = 0;//置为不可激活状态
//						System.out.println(vk+"置为不可激活状态");
						chooseActions.add(tempAction);//将action加入到chooseActions中
//						System.out.println("chooseAction为："+tempAction.start+"->"+tempAction.end+"tempAction的able为"+tempAction.able);
//						System.out.println("节点vk = " + vk + "加入到独立集");
						if(main.addToIS(vk, t)) {//加入到独立集
							main.IS.get(t-1).add(vk);
						}

//						System.out.println();
						HashMap<String, Object> hashmap2 = new HashMap<String, Object>();
						hashmap2 = main.chooseNextVertexByProbability(vk, activable);
						vj = (int)hashmap2.get("next");//选择下一个
//						System.out.println("vj："+vj);
						tempAction = (Action) hashmap2.get("action");
						vkIsVt = false;
					} else {
//						System.out.println("vk == vt!!!");
						vj = main.chooseNextRandomly(activable, vertexT);//随机选择下一个==========此处改为用行为概率向量
//						System.out.println("随机得到的vj："+vj);
						vkIsVt = true;
					}
				} else {
//					System.out.println("vj == vt!!!");
					int vk = main.chooseNextRandomly(activable, vertexT);//随机选择下一个==========此处改为用行为概率向量
//					System.out.println("随机得到的vk："+vk);
					if(vk != vt) {
						activable[vk] = 0;//置为不可激活状态
//						System.out.println(vk+"置为不可激活状态");
//						System.out.println("节点vk = " + vk + "加入到独立集");
						if(main.addToIS(vk, t)) {//加入到独立集
							main.IS.get(t-1).add(vk);
						}

//						System.out.println();
						HashMap<String, Object> hashmap3 = new HashMap<String, Object>();
						hashmap3 = main.chooseNextVertexByProbability(vk, activable);
						vj = (int)hashmap3.get("next");//选择下一个
//						System.out.println("vj："+vj);
						tempAction = (Action) hashmap3.get("action");
						vkIsVt = false;
					} else {
						System.out.println("！！！！！！！注意此时vj和vk都为vt！！！！！！！！错误");
						vj = main.chooseNextRandomly(activable, vertexT);//随机选择下一个
//						System.out.println("随机得到的vj："+vj);
						vkIsVt = true;
					}
				}
			}
			if(t == 1) {
				//计算阈值OMEGA
				double OMEGA = main.getThreshold(t);
				System.out.println("IS(t).size = "+main.IS.get(t-1).size());
				if(OMEGA <= main.IS.get(t-1).size()) {
					main.reward(chooseActions);
				} else {
					main.penalize(chooseActions);
				}
				System.out.println();
				t++;
			} else {
				if(main.IS.get(t-1).size() >= main.IS.get(t-2).size()) {//第t次迭代的结果对于上一次来讲有改进
					//计算阈值OMEGA
					double OMEGA = main.getThreshold(t);
					System.out.println("IS(t).size = "+main.IS.get(t-1).size());
					System.out.println("比上一次迭代结果更好：IS = "+main.IS+"\n");
					if(OMEGA <= main.IS.get(t-1).size()) {
						main.reward(chooseActions);
					} else {
						main.penalize(chooseActions);
					}
					t++;
				} else {//当此次迭代结果比上一次得到的独立集的个数更少,此时t不进行自增加操作，清空IS，重新迭代
//					System.out.println("此次迭代结果为："+ main.IS +"比上一次得到的独立集的个数更少");
//					main.graph = lastGraph;
//					ArrayList<Integer> lastIS = new ArrayList<Integer>(main.IS.get(t-1).size());
//					for(Integer element:main.IS.get(t-1)) {
//						lastIS.add(element);
//					}
//					main.IS.get(t-1).removeAll(lastIS);
//					Iterator<Integer> iterator2 = main.IS.get(t-1).iterator();
//					while(iterator2.hasNext()) {
//						iterator2.remove();
//					}

					ArrayList<Integer> lastIS = new ArrayList<Integer>(main.IS.get(t-1).size());
					for(Integer element:main.IS.get(t-1)) {
						lastIS.add(element);
					}


					//计算阈值OMEGA
					double OMEGA = main.getThreshold(t);
					System.out.println("IS(t).size = "+main.IS.get(t-1).size());
					System.out.println("比上一次迭代结果更差：IS = "+main.IS+"\n");
					if(OMEGA <= main.IS.get(t-1).size()) {
						main.reward(chooseActions);
					} else {
						main.penalize(chooseActions);
					}

					main.IS.get(t-1).removeAll(lastIS);
					main.IS.get(t-1).addAll(main.IS.get(t-2));
					t++;
				}
			}
		}
		for(int i = 0 ; i < main.IS.size(); i++) {
			System.out.println(main.IS.get(i).size());
		}
	}
	
	void readGraph() {
		this.graph = new Graph();
		this.graph.vertexes = new ArrayList<Vertex>(N);
		
		File file = new File("d:/frb200.txt");
		
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file));
            int line = 0;
            int count = 0;
            int tempchar;
            
           
            while ((tempchar = reader.read()) != -1) {
				if ((char) tempchar != ' ' && tempchar != 10 && tempchar != 13) {//在ASCII码中，13代表回车键，10代表换行键
					if (count == 0) {
						Vertex v = new Vertex();
						v.order = line;
						v.neighbours = new ArrayList<Integer>();
						v.la = new ArrayList<Action>();
						this.graph.vertexes.add(v);

						if ((char) tempchar == '1') {
							this.graph.vertexes.get(line).neighbours.add(count);
							count++;
						} else if ('0' == (char) tempchar) {
							count++;
						}
					} else if (count == (N - 1)) {
						if ((char) tempchar == '1') {
							this.graph.vertexes.get(line).neighbours.add(count);
						}
						count = 0;
						++line;
					} else {
						if ((char) tempchar == '1') {
							this.graph.vertexes.get(line).neighbours.add(count);
							count++;
						} else if ((char) tempchar == '0') {
							count++;
						}
					}
				}
			}
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }        
	}
	
	void init() {
		this.IS = new ArrayList<HashSet<Integer>>(Loop);
		for(int i  = 0; i < Loop; i++) {
            HashSet<Integer> temp = new HashSet<Integer>();
            this.IS.add(temp);
        }
		for(Vertex v : this.graph.vertexes) {
			for(int v1 : v.neighbours) {				
				Action ac = new Action();
				ac.start = v.order;
				ac.end = v1;
				ac.able = true;
				ac.prob = (1.0/(double)v.neighbours.size());
				graph.vertexes.get(v.order).la.add(ac);//添加到order点的la集合中

			}
		}
		
	}
	
	void disableActionsWithVertex(int order) {//禁用所有与order相连接的la的指向order的action
//		System.out.println("禁用通向"+order+"的actions");

		Iterator<Action> iterator1 = this.graph.vertexes.get(order).la.iterator();
		while(iterator1.hasNext()) {
			Action ac = iterator1.next();
			int neighbourVertex = ac.end;
			Iterator<Action> iterator2 = this.graph.vertexes.get(neighbourVertex).la.iterator();
			int count = 0;
			while(iterator2.hasNext()) {
				Action action1 = iterator2.next();
				if (action1.end == order) {
//					System.out.println("被禁用的action的start = "+action1.start+"，end = "+action1.end);
					this.graph.vertexes.get(neighbourVertex).la.get(count).able = false;
				}
				count++;
			}
		}
	}

	boolean addToIS(int order, int t) {//将点order加入到独立集中，如果该点加入后导致现有独立集不再互相独立，不添加该点，并返回false
		for(int element : this.IS.get(t-1)) {
			if(element == order) {
//				System.out.println("当前独立集中已存在"+ order +"点，添加失败");
				return false;
			}
		}

		for(int element : this.IS.get(t-1)) {
			for(int neighbour : this.graph.vertexes.get(element).neighbours) {
				if(neighbour == order) {
//					System.out.println(order +"点是当前独立集中的元素的邻居，添加失败");
					return false;
				}
			}
		}
		System.out.println(order+"添加成功");
		return true;
	}

	HashMap<String, Object> chooseNextVertexByProbability(int order, int[] activable) {//根据概率向量选择下一个action
		HashMap<String, Object> result = new HashMap<String, Object>();
		Action action = new Action();
		Integer next = 0;
		double prob = 0;
		
		for(Action action1 : this.graph.vertexes.get(order).la) {
//			System.out.println(action1.end+" "+action1.able+" "+action1.prob);

			if(action1.able == true) {
				if(action1.prob > prob && activable[action1.end] == 1) {//概率更大并且未被激活过
//					System.out.println(action1.end+" "+action1.prob);
					prob = action1.prob;
					next = action1.end;
				}
			}
		}
		//当prob都是零的时候，下一个点选择vt
		if(prob == 0) {
			next = vt;//选vt
		}
		action.start = order;
		action.end = next;
		action.able = true;
		action.prob = prob;

//		System.out.println("下一个点是"+next);

		result.put("next", next);
		result.put("action", action);
		return result;
	}
	
	boolean judgeActivableNumber(int[] activable) {
		int number = 0;
		for(int x : activable) {
			if(x == 1) {
				number++;
			}
		}
		if(number == 1) {
			System.out.println("只有一个可以被激活");
			return false;
		} else if(number > 1) {
			return true;
		} else {
			System.out.println("出错@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
			return false;
		}
		
	}

	int chooseNextRandomly(int[] activable, Vertex vertexT) {
		int next = 0;
		int numberOf1 = 0;
		ArrayList<Integer> active = new ArrayList<Integer>();
		for(int i = 0; i < activable.length; i++) {
			if(activable[i] == 1) {
				numberOf1 ++;
				active.add(i);
			}
		}

//		System.out.println("可以被激活的点的总数为："+numberOf1);
//		Random rand = new Random();
//		int temp = rand.nextInt(numberOf1);
//		System.out.println("随机数为："+temp);
//		next = active.get(temp);
		double prob = 0.0;
		for(Action action : vertexT.la) {
			if(prob < action.prob && activable[action.end] == 1) {
				prob = action.prob;
				next = action.end;
			}
		}

//		System.out.println("从vt选择的下一个点是："+next);
		return next;
	}
	
	double getThreshold(int t) {
		double omega = 0.0;
        double sum = 0.0;
		for(int i = 1; i <= t; i++) {
            sum += (double) this.IS.get(i-1).size();
        }
        omega = sum/(double)t;
		System.out.println("得到的阈值为："+omega);
		return omega;
	}

	void reward(ArrayList<Action> chooseActions) {
       	System.out.println("奖励~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        Iterator<Action> iterator = chooseActions.iterator();
//		HashSet<Integer> startVertexesOfChooseActions = new HashSet<Integer>();//所有被选过的action的开始节点的不重复集合
//		HashSet<Integer> endVertexesOfChooseActions = new HashSet<Integer>();//所有被选过的action的结束节点的不重复集合

		while(iterator.hasNext()) {
			Action action = iterator.next();
			Iterator<Action> iterator1 = this.graph.vertexes.get(action.start).la.iterator();
			int count = 0;
			while(iterator1.hasNext()) {//此action所在的LA的每个action1进行奖励
				Action action1 = iterator1.next();

				if(action1.end == action.end) {//这个提高
					double temp = action1.prob;
					this.graph.vertexes.get(action.start).la.get(count).prob = action1.prob + reward*(1.0 - action1.prob);
					double result = this.graph.vertexes.get(action.start).la.get(count).prob;
//					System.out.println("action:"+action1.start+" -> "+action1.end+"奖励后的概率提高了："+(result-temp));
				} else {//这个降低
					double temp = action1.prob;
					this.graph.vertexes.get(action.start).la.get(count).prob = (1.0 - reward)*action1.prob;
					double result = this.graph.vertexes.get(action.start).la.get(count).prob;
//					System.out.println("action:"+action1.start+" -> "+action1.end+"奖励后的概率降低了："+(temp -result));
				}
				count++;
			}
		}
	}

	void penalize(ArrayList<Action> chooseActions) {
		System.out.println("惩罚！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！");
		Iterator<Action> iterator = chooseActions.iterator();
		while(iterator.hasNext()) {
			Action action = iterator.next();
			Iterator<Action> iterator1 = this.graph.vertexes.get(action.start).la.iterator();
			int count = 0;
			while (iterator1.hasNext()) {//此action所在的LA的每个action1进行奖励
				Action action1 = iterator1.next();

				if (action1.end == action.end) {//这个降低
					double temp = action1.prob;
					this.graph.vertexes.get(action.start).la.get(count).prob = (1.0 - penalty)*action1.prob;
					double result = this.graph.vertexes.get(action.start).la.get(count).prob;
//					System.out.println("action:"+action1.start+" -> "+action1.end+"惩罚后的概率降低了：" + (temp-result));
				} else {//这个提高
					double temp = action1.prob;
					int r = this.graph.vertexes.get(action.start).la.size();

					this.graph.vertexes.get(action.start).la.get(count).prob = penalty/(double)(r - 1) + (1.0-penalty)*action1.prob;
					double result = this.graph.vertexes.get(action.start).la.get(count).prob;
//					System.out.println("action:"+action1.start+" -> "+action1.end+"惩罚后的概率提高了：" + (result-temp));
				}

				count++;
			}
		}
	}

	double productOfMaximumProbability(int t) {
		double 	product = 0;
		if(t == 0) {
			product = 0;
		} else {
			
		}
		
		return product;
	}
}