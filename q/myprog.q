mutable int main(int arg) {
    if (arg == 1) {
        mutable Ref list = nil;
        mutable int i = 0;
        while (i < 17) {
            list = i . list;
            i = i + 1;
        }
        return 0;
    }
    
    if (arg == 2) {
        mutable Ref list = nil;
        mutable int i = 0;
        while (i < 8) {
            mutable Ref a = i . nil;
            mutable Ref b = i . nil;
            setRight(a, b);
            setRight(b, a);
            i = i + 1;
        }
        
        i = 0;
        while (i < 8) {
            list = i . list;
            i = i + 1;
        }
        return 0;
    }
    
    if (arg == 3) {
        mutable Ref list = nil;
        mutable int i = 0;
        while (i < 16) {
            list = i . list;
            i = i + 1;
            if (i == 8) {
                list = nil;
            }
        }
        return 0;
    }
    
    if (arg == 4) {
        mutable Ref list = nil;
        mutable int i = 0;
        while (i < 20) {
            list = i . nil;
            i = i + 1;
        }
        return 0;
    }
    
    return 0;
}
