#include "TreeNode.hpp"

using Node_ptr = TreeNode*;

TreeNode::TreeNode(const std::string& name, std::string value, Node_ptr parent, size_t height)
	: _name{ name },
	_value{ value },
	_parent{ parent },
	_height{ height }
{}

// Depth-First Traversal
Node_ptr TreeNode::traverse(std::function<bool(Node_ptr)> action)
{
	std::deque<Node_ptr> deck;
	this->addAllToDeque(deck);
	while (!deck.empty())
	{
		Node_ptr ptr = deck.back();
		deck.pop_back();
		if (action(ptr))
		{
			return ptr;
		}
		if (!ptr->isLeaf())
		{
			ptr->addAllToDeque(deck);
		}
	}
	return nullptr;
}

Node_ptr TreeNode::search(const TreeNode& node)
{
	return this->traverse([&node](Node_ptr treeNode) { return node == *treeNode; });
}

std::string TreeNode::toJSON() const
{
	std::string json{ '{' };

	for (const auto& node : this->_children)
	{
		if (node.isLeaf())
		{
			json += "\"";
			json += node._name;
			json += "\":";
			json += node._value;
		}
		else
		{
			json += "\"";
			json += node._name;
			json += "\":";
			json += node.toJSON();
		}
		json += ',';
	}
	if (json.length() > 1)
	{
		json.back() = '}';
	}
	else
	{
		json += '}';
	}
	return json;
}

TreeNode::TreeNode(const char* name, Node_ptr parent)
	: TreeNode(std::string{ name }, "", parent)
{}

TreeNode::TreeNode(const std::string& name, Node_ptr parent)
	: TreeNode(name, "", parent)
{}

TreeNode::TreeNode(const std::string& name, std::string value, Node_ptr parent)
	: TreeNode(std::string{ name }, value, parent, parent == nullptr ? 0 : 1)
{}

TreeNode::TreeNode(TreeNode&& node)
	: _name{ std::move(node._name) },
	_value{ std::move(node._value) },
	_parent{ std::move(node._parent) },
	_height{ std::move(node._height) },
	_children{ std::move(node._children) }
{
	for (TreeNode& node : this->_children)
	{
		node._parent = this;
	}
}

bool TreeNode::operator == (const TreeNode& that) const
{
	return this->_name == that._name && this->_parent == that._parent;
}

bool TreeNode::operator != (const TreeNode& that) const
{
	return this->_name != that._name || this->_parent != that._parent;
}

const Node_ptr TreeNode::find(const TreeNode& node)
{
	return this->search(node);
}

// Returns the newly inserted node
Node_ptr TreeNode::insert(const TreeNode& node)
{
	Node_ptr ptr = this->search(node);
	if (ptr == nullptr)
	{
		this->_children.emplace_back(node._name, node._value, this, this->_height + 1);
		ptr = &this->_children.back();
	}
	return ptr;
}

const std::string& TreeNode::getName() const
{
	return this->_name;
}

const std::string& TreeNode::getValue() const
{
	return this->_value;
}

const std::vector<TreeNode>& TreeNode::children() const
{
	return this->_children;
}

bool TreeNode::isLeaf() const
{
	return this->_children.size() == 0;
}

bool TreeNode::isRoot()
{
	return this->_parent == nullptr;
}

const TreeNode& TreeNode::getParent() const
{
	return *(this->_parent);
}

int TreeNode::height()
{
	return this->_height;
}