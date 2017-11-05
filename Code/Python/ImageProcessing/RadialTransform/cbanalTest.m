function b=cbanalTest(slika)
	b = zeros(20,20);

	for i=1:20
		b(i,:) = cbanal(slika(i,:));
	end;

	for j=1:20
		b(:,j) = cbanal(b(:,j));
	end;
