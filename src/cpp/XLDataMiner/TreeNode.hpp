#ifndef _TREENODE_H_INCLUDED_
#define _TREENODE_H_INCLUDED_

#include <string>
#include <vector>
#include <deque>
#include <functional>

class TreeNode
{
public:
	using Node_ptr = TreeNode*;
	TreeNode(const char*, Node_ptr);
	TreeNode(const std::string&, Node_ptr);
	TreeNode(const std::string&, std::string, Node_ptr);
	TreeNode(const std::string&, std::string, Node_ptr, size_t);

	TreeNode(TreeNode&&);

	bool operator == (const TreeNode&) const;
	bool operator != (const TreeNode&) const;

	const Node_ptr find(const TreeNode&);

	// Returns the newly inserted node
	Node_ptr insert(const TreeNode&);

	const std::string& getName() const;

	const std::string& getValue() const;

	const std::vector<TreeNode>& children() const;

	bool isLeaf() const;

	bool isRoot();

	const TreeNode& getParent() const;

	int height();

	std::string toJSON() const;

protected:
	std::string _name;
	std::string _value;
	Node_ptr _parent;
	std::vector<TreeNode> _children;
	size_t _height = 0;

	Node_ptr traverse(std::function<bool(Node_ptr)>);
	Node_ptr search(const TreeNode&);

	inline void addAllToDeque(std::deque<Node_ptr>& deck)
	{
		std::vector<TreeNode>::reverse_iterator it;
		for (it = this->_children.rbegin(); it != this->_children.rend(); ++it)
		{
			deck.emplace_back(&(*it));
		}
	}
};
#endif // _TREENODE_H_INCLUDED_