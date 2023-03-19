package com.wildbitsfoundry.caveman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.LinkedList;
import java.lang.StringBuilder;
import java.util.function.Predicate;

public class CaveTree {
	// Need to find a place for these Ref classes
	public static class Ref<T> {
		protected T _value;

		public Ref(T value) {
			this._value = value;
		}

		public T get() {
			return _value;
		}

		public void set(T value) {
			this._value = value;
		}

		@Override
		public String toString() {
			return this._value.toString();
		}

		@Override
		public boolean equals(Object o) {
			return this._value.equals(o);
		}

		@Override
		public int hashCode() {
			return this._value.hashCode();
		}
	}

	public static class IntRef extends Ref<Integer> {
		public IntRef(Integer value) {
			super(value);
		}

		public IntRef() {
			super(0);
		}

		public void add(int value) {
			this._value += value;
		}
	}

	protected CaveTree _parent;
	protected String _name;
	protected String _value;
	protected int _height;
	protected List<CaveTree> _children;

	private IntRef _charPtr;

	public CaveTree(String json) {
		this._name = "root";
		this._value = null;
		this._parent = null;
		this._height = 0;

		this._charPtr = new IntRef();

		this._children = json.equals("{}") ? new ArrayList<>(0) : this.tokenize(json, this._charPtr);
	}

	protected CaveTree(String name, String value, CaveTree parent) {
		this._name = name;
		this._value = value;
		this._parent = parent;
		this._height = parent == null ? 0 : parent._height + 1;
		this._children = new ArrayList<>(0);
	}

	private String parseString(String json, Ref<Integer> index) {
		int ptr = index.get();
		++ptr;
		int begin = ptr;
		while (json.charAt(ptr) != '"') {
			++ptr;
		}
		String value = json.substring(begin, ptr);
		++ptr;
		index.set(ptr);
		return value;
	}

	private String parseNumber(String json, Ref<Integer> index) {
		int ptr = index.get();
		int begin = ptr;
		while (isNumeric(json.charAt(ptr))) {
			++ptr;
		}
		String value = json.substring(begin, ptr);
		index.set(ptr);
		return value;
	}

	private String parseBoolean(String json, Ref<Integer> index) {
		int ptr = index.get();
		int begin = ptr;
		while (json.charAt(ptr) != 'e' && json.charAt(ptr) != 'E') {
			++ptr;
		}
		++ptr;
		String value = json.substring(begin, ptr);
		index.set(ptr);
		return value;
	}

	private String parseArray(String json, Ref<Integer> index) {
		int ptr = index.get();
		++ptr;
		int begin = ptr;
		while (json.charAt(ptr) != ']') {
			++ptr;
		}
		++ptr;
		String value = json.substring(begin - 1, ptr);
		index.set(ptr);
		return value;
	}

	private List<CaveTree> tokenize(String json, IntRef index) {
		List<CaveTree> members = new ArrayList<>();
		String name = null;

		char token;
		for (;;) {
			token = json.charAt(index.get());
			if (token == '{') {
				index.add(1);
				if (name == null) {
					name = parseString(json, index);
				} else {
					CaveTree node = new CaveTree(name, null, this);
					node._children = node.tokenize(json, index);
					members.add(node);
					index.add(1);
					token = json.charAt(index.get());
				}
			}
			if (token == '}') {
				return members;
			} else if (token == ':') {
				index.add(1);
			} else if (token == '"') {
				if (name != null) {
					String value = parseString(json, index);
					CaveTree node = new CaveTree(name, value, this);
					members.add(node);
				} else {
					name = parseString(json, index);
				}
			} else if (token == ',') {
				index.add(1);
				name = null;
			} else if (isDigitOrDotOrMinus(token)) {
				String value = parseNumber(json, index);
				CaveTree node = new CaveTree(name, value, this);
				members.add(node);
				name = null;
			} else if (token == 'T' || token == 't' || token == 'F' || token == 'f') {
				String value = parseBoolean(json, index);
				CaveTree node = new CaveTree(name, value, this);
				members.add(node);
				name = null;
			} else if (token == '[') {
				// Not fully i.e. tested =]. Array of objects
				if (json.charAt(index.get() + 1) == '{') {
					index.add(1);
					while (json.charAt(index.get()) != ']') {
						CaveTree node = new CaveTree(name, null, this);
						members.add(node);
						node._children = tokenize(json, index);
						index.add(1);
					}
					return members;
				} else {
					String value = parseArray(json, index);
					CaveTree node = new CaveTree(name, value, this);
					members.add(node);
					name = null;
				}
			} else {
				index.add(1);
			}
		}
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isDigitOrDotOrMinus(char c) {
		return isDigit(c) || c == '.' || c == '-';
	}

	private static boolean isNumeric(char c) {
		return isDigitOrDotOrMinus(c) || c == 'e' || c == 'E' || c == '+' || c == '-';
	}

	private CaveTree traverseDepthFirst(Predicate<CaveTree> action) {
		Deque<CaveTree> deck = new LinkedList<CaveTree>();
		addAllInReverse(deck, this._children);
		while (!deck.isEmpty()) {
			CaveTree node = deck.pollLast();
			if (action.test(node)) {
				return node;
			}
			if (!node.isLeaf()) {
				addAllInReverse(deck, node._children);
			}
		}
		return null;
	}

	private static void addAllInReverse(Deque<CaveTree> deck, List<CaveTree> nodes) {
		for (int i = nodes.size() - 1; i >= 0; --i) {
			deck.addLast(nodes.get(i));
		}
	}

	private CaveTree traverseBreadthFirst(Predicate<CaveTree> action) {
		Deque<CaveTree> deck = new LinkedList<>();
		deck.addAll(this._children);
		while (!deck.isEmpty()) {
			CaveTree node = deck.pollFirst();
			if (action.test(node)) {
				return node;
			}
			if (!node.isLeaf()) {
				deck.addAll(node._children);
			}
		}
		return null;
	}

	public double getAsDouble() {
		return Double.parseDouble(this._value);
	}

	public int getAsInteger() {
		return Integer.parseInt(this._value);
	}

	public String getAsString() {
		return this._value;
	}

	public double[] getAsDoubleArray() {
		List<String> tmp = CaveTools.StringSplitterToList(this._value.substring(1, this._value.length() - 1), ',');
		double[] result = new double[tmp.size()];
		for (int i = 0; i < tmp.size(); ++i) {
			result[i] = Double.parseDouble(tmp.get(i));
		}
		return result;
	}

	public String[] getAsStringArray() {
		return CaveTools.StringSplitter(this._value.substring(1, this._value.length() - 1), ',');
	}

	public boolean getAsBoolean() {

		if(this._value.equals("TRUE") || this._value.equals("true")) {
			return true;
		}
		if(this._value.equals("FALSE") || this._value.equals("false")) {
			return false;
		}
		throw new DataConversionException("Cannot convert " + this._value + " to boolean");
	}

	public CaveTree children(String name) {
		return this.find(name);
	}

	public CaveTree find(String name) {
		String[] nodeNames = CaveTools.StringSplitter(name, '.');
		CaveTree runningNode = this;
		for (String nodeName : nodeNames) {
			Predicate<CaveTree> action = node -> node._name.equals(nodeName);
			runningNode = runningNode.traverseBreadthFirst(action);
			if (runningNode == null) {
				throw new KeyNotFoundException(String.format("Could not find key: [%s].", name));
			}
		}
		return runningNode;
	}

	public boolean isRoot() {
		return this._parent == null;
	}

	public boolean isLeaf() {
		return this._children.size() == 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this._name).append(":").append(System.lineSeparator());
		Predicate<CaveTree> action = node -> {
			int height = node._height;
			while (height > 0) {
				sb.append(" ");
				--height;
			}
			sb.append("|-> ").append(node._name).append(" : ");
			if (node.isLeaf()) {
				if(isNumeric(node._value) || isArray(node._value) || isBoolean(node._value)) {
					sb.append(node._value);
				} else {
					sb.append("\"").append(node._value).append("\"");
				}

			}
			sb.append(System.lineSeparator());
			return false;
		};
		this.traverseDepthFirst(action);
		return sb.toString();
	}

	public static boolean isNumeric(String str) {
		return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}

	public static boolean isArray(String str) {
		return str.charAt(0) == '[' && str.charAt(str.length() - 1) == ']';
	}

	public static boolean isBoolean(String str) {
		return str.equals("true") || str.equals("TRUE") || str.equals("false") || str.equals("FALSE");
	}

	public String toJSON() {
		StringBuilder json = new StringBuilder("{");

		for (CaveTree node : this._children) {
			if (node.isLeaf()) {
				json.append("\"");
				json.append(node._name);
				json.append("\":");
				if (node._value.indexOf(',') > 0) {
					json.append("[").append(node._value).append("]");
				} else {
					json.append(node._value);
				}
			} else {
				json.append("\"");
				json.append(node._name);
				json.append("\":");
				json.append(node.toJSON());
			}
			json.append(',');
		}
		if (json.length() > 1) {
			json.setCharAt(json.length() - 1, '}');
		} else {
			json.append('}');
		}
		return json.toString();
	}

	public static void main(String[] args) {
		String json = "{\"a_c\":1, \"b\":[1,1,1,1],\"a\":{\"b\":-5.0,\"c\":2.0},\"c\":{\"b\":{\"d\":{\"a\":3.0}},\"a\":{\"b\":2.0}},\"function\":0.0,\"flag\":true,\"other_flag\":false}";
		CaveTree root = new CaveTree(json);
		System.out.printf("b[] = %s\n", Arrays.toString(root.find("b").getAsDoubleArray()));
		System.out.printf("a.get(b) = %f\n", root.find("a").children("b").getAsDouble());
		System.out.printf("a.c = %f\n", root.find("a.c").getAsDouble());
		System.out.printf("c.a.b = %f\n", root.find("c.a.b").getAsDouble());
		System.out.printf("c.b.d.a = %f\n", root.find("c.b.d.a").getAsDouble());
		System.out.printf("flag = %b\n", root.find("flag").getAsBoolean());
		System.out.println(root.toString());
	}
}