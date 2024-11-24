// Problem 1
int isList(Q q) {
    if (isAtom(q)) {
        if (q == nil) {
            return 1;
        } else {
            return 0;
        }
    } else {
        return isList(right(q));
    }
}

// Problem 2
Ref append(Ref list1, Ref list2) {
    if (list1 == nil) {
        return list2;
    } else {
        return (left(list1) . append(right(list1), list2));
    }
}

// Problem 3
Ref reverse(Ref list) {
    if (list == nil) {
        return nil;
    } else {
        return append(reverse(right(list)), (left(list) . nil));
    }
}

// Problem 4
int isSorted(Ref listOfLists) {
    if (listOfLists == nil || right(listOfLists) == nil) {
        return 1;
    } else {
        Ref firstList = left(listOfLists);
        Ref secondList = left(right(listOfLists));
        if (length(firstList) <= length(secondList)) {
            return isSorted(right(listOfLists));
        } else {
            return 0;
        }
    }
}

int length(Ref list) {
    if (list == nil) {
        return 0;
    } else {
        return 1 + length(right(list));
    }
}

/* Problem 5:
If statements are crucial for making immutable Quandary Turing-complete. Without if statements, 
it would be impossible to implement conditional logic or terminate recursion, which are essential 
for complex computations. None of the functions in the previous problems could be implemented 
without if statements, as they all rely on conditional checks.

Recursion is equally important for making immutable Quandary Turing-complete. Without recursion, 
it would be impossible to implement loops or process data structures. Many functions and all the 
functions in the previous problems require recursion. In an immutable language without while 
loops, recursion is the only way to implement iterative processes.
*/
