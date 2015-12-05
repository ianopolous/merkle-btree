# merkle-btree
A content addressed B-tree backed by a content addressed hashtable.

Each tree node is stored as an object in the content addressed storage, and contains links to its children. Each link is a hash which can be loooked up from the content addressed storage. 
