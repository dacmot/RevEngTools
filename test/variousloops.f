        integer function sigprod(n)
        integer n, i
        double precision s, w
        s = 1.0
        w = 1.0
        do 10, i=1, n
           w = w / i
           s = s + w
   10   continue 
        sigprod = s
        end

        double precision function ff2(n)
        integer n, i, fact
        double precision sum1
        sum1 = 0.0
        fact = 1.0
        do 60, i=1, n
           fact = fact * i
           sum1 = sum1 + 1/fact
   60   continue
        ff2 = sum1
        end

        integer function sigsum(n)
        integer n, i
        double precision s, w
        s = 1.0
        w = 1.0
        do 20, i=1, n
           w = w * i
           s = s + w
   20   continue
        sigsum = s
        end

        integer function sigsum1(n)
        integer n, i, s
        s=n
        do 30, i=n-1, 1, -1
           s = i * (1 + s)
   30   continue
        sigsum = s+1
        end

        integer function chebyshev(n,x)
        integer n, x, i, u0, u1, v
        u0 = 1
        u1 = x
        do 50, i=2, n
           v = u1
           u1 = -u0 + 2*x*u1
           u0 = v
   50   continue
        chebyshev = u1
        end

        double precision function bessel(z, nu, m)
        integer i, z, nu, m
        double precision res, u
        res = 0
        u = 1
        do 40, i=0, m-1
           res = res + u
           u = -u*z**2 / (4*(i+nu+1)*(i+1))
   40   continue
        bessel = res
        end

        double precision function mystery(z, nu, m)
        integer z, nu, m, i
        double precision res, t
        res = 0
        t = 1
        do 70, i=0, m
           res = res + t
           t = -t*z**2 / (4*(i+nu+1)*(i+1))
   70   continue
        mystery = res
        end
