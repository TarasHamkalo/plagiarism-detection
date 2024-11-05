function clear_indexes() {
  docker stop plagdet-db
  rm /home/taras-hamkalo/repositories/plagiarism-detection/SIMPLE_FS_INDEX/*
  rm /home/taras-hamkalo/repositories/plagiarism-detection/lucene_index/*
  mkdir -p /home/taras-hamkalo/repositories/plagiarism-detection/lucene_index/
  mkdir -p /home/taras-hamkalo/repositories/plagiarism-detection/SIMPLE_FS_INDEX/
  docker volume rm pg_data
  docker volume create pg_data
}

clear_indexes

docker run -d --rm \
  --name plagdet-db \
  -e POSTGRES_USER=taras \
  -e POSTGRES_PASSWORD=taras \
  -e POSTGRES_DB=plagdet \
  -v pg_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres
#  --net gamestudio-net \
