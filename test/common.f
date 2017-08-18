        program main
C
        common /XYZ/ b, c, a, d
        real a
        dimension b(10)
        double precision c
        integer d
C
        a = 8
        c = 69.0
        call nublet()
C
        end
C
C
C
        subroutine nublet()
C
        common /XYZ/ b(10), c, d, a
        double precision c
        integer d
C
        write (*,*) 'nublet: ', c, ',', a
C
        end
