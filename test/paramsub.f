        program main
        external test
        call sub2(test)
        end
c
        subroutine sub2(f)
        mod = f()
        end
c
        function test()
        stfunc(a,b) = (a * b)**2
        test = mod(int(stfunc(3,4)), 5)
        end
