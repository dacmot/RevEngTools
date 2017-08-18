        program main
        integer a(10), b
        double precision c
        common /block/ b, c

        call array(a)
        end

        subroutine array(d)
        integer d(1), b
        double precision c
        common /block/ b, c

        d(3) = 5
        end
