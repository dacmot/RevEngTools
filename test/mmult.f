        subroutine mmult1(x, y, z, m, n, p)

        integer i, j, k
        integer m, n, p
        integer x(m, p), y(p, n), z(m, n)

        do 10 i=1, m
           do 20 j=1, n
              do 30 k=1, p
                 z(i,j) = z(i,j) + x(i,k) * y(k, j)
   30         continue
   20      continue
   10   continue

        end

        subroutine mmult2(x, y, z, m, n, p)

        integer i, j, k, zz
        integer m, n, p
        integer x(m, p), y(p, n), z(m, n)

        do 10 i=1, m
           do 20 j=1, n
              zz = 0
              do 30 k=1, p
                 zz = zz + x(i,k) * y(k, j)
   30         continue
              z(i,j) = zz
   20      continue
   10   continue

        end
