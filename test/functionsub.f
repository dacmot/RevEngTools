        program main
        integer a,b,c
        external fct
        a = 1
        b = 2
        write (*, *), fct(b)
        c = fct(b) + 100
        write (*,*), a, b, c
        end

        function fct(arg)
        integer arg,fct
        arg = arg * 10
        fct = arg
        return
        end
