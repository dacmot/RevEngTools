        program main
C
        common // b, c, a, d
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
        common // g(10), e, f
        double precision e
        real f
C
        write (*,*) 'nublet: ', e, ',', f
C
        end
